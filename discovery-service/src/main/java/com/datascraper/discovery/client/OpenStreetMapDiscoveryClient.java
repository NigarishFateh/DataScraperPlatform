package com.datascraper.discovery.client;

import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import com.datascraper.discovery.dto.WebSearchHit;
import com.datascraper.discovery.support.WebsiteUrlSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Discovers local businesses via Photon (OpenStreetMap) + Nominatim website lookup.
 * Used when HTML search engines (DuckDuckGo) are blocked or empty for non-tech industries.
 */
@Component
public class OpenStreetMapDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(OpenStreetMapDiscoveryClient.class);
    private static final String USER_AGENT = "DataScraperPlatform/0.1 (local company discovery)";
    private static final int MAX_CITIES = 6;
    private static final int MAX_KEYWORDS = 2;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper objectMapper;

    public OpenStreetMapDiscoveryClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<WebSearchHit> discover(ResolvedDiscoveryCriteria criteria) {
        List<WebSearchHit> hits = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        List<String> keywords = limit(criteria.searchKeywords(), MAX_KEYWORDS);
        String country = criteria.countryNames().isEmpty() ? "" : criteria.countryNames().get(0);
        int cityLimit = Math.min(criteria.cityNames().size(), MAX_CITIES);

        List<CityGeo> cities = new ArrayList<>();
        for (int i = 0; i < cityLimit; i++) {
            String cityName = criteria.cityNames().get(i);
            if (cityName == null || cityName.isBlank()) {
                continue;
            }
            String cityId = i < criteria.cityIds().size() ? criteria.cityIds().get(i) : null;
            cities.add(new CityGeo(cityId, cityName));
        }
        if (cities.isEmpty() && !country.isBlank()) {
            cities.add(new CityGeo(null, country));
        }

        for (CityGeo city : cities) {
            for (String keyword : keywords) {
                if (keyword == null || keyword.isBlank()) {
                    continue;
                }
                String query = (keyword + " " + city.cityName() + " " + country).trim().replaceAll("\\s+", " ");
                try {
                    List<WebSearchHit> batch = searchPhoton(query, city, criteria, seen);
                    log.info("OpenStreetMap/Photon '{}' -> {} places", query, batch.size());
                    hits.addAll(batch);
                } catch (Exception ex) {
                    log.warn("OpenStreetMap discovery failed for '{}': {}", query, ex.getMessage());
                }
                if (hits.size() >= criteria.maxResults()) {
                    return hits;
                }
                sleepQuietly(300);
            }
        }
        return hits;
    }

    private List<WebSearchHit> searchPhoton(
            String query,
            CityGeo city,
            ResolvedDiscoveryCriteria criteria,
            Set<String> seen
    ) throws Exception {
        String url = "https://photon.komoot.io/api/?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&limit=15";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Photon HTTP " + response.statusCode());
        }

        JsonNode features = objectMapper.readTree(response.body()).path("features");
        List<WebSearchHit> hits = new ArrayList<>();
        if (!features.isArray()) {
            return hits;
        }

        for (JsonNode feature : features) {
            JsonNode props = feature.path("properties");
            String name = text(props, "name");
            if (name.isBlank()) {
                continue;
            }
            String featureCity = firstNonBlank(text(props, "city"), text(props, "county"), city.cityName());
            String featureCountry = text(props, "country");
            if (!matchesGeo(featureCity, featureCountry, city.cityName(), criteria)) {
                continue;
            }

            String osmType = text(props, "osm_type");
            String osmId = props.path("osm_id").asText("");
            String dedupe = (osmType + ":" + osmId + ":" + name).toLowerCase(Locale.ROOT);
            if (!seen.add(dedupe)) {
                continue;
            }

            String website = lookupWebsite(osmType, osmId);
            website = WebsiteUrlSupport.normalizeHttpUrl(website);
            if (!WebsiteUrlSupport.isUsableCompanyWebsite(website)) {
                log.debug("Skipping OSM place '{}' — no company website", name);
                sleepQuietly(1100);
                continue;
            }

            hits.add(new WebSearchHit(
                    name,
                    website,
                    website,
                    firstCountry(criteria),
                    city.cityId(),
                    firstNonBlank(featureCity, city.cityName()),
                    "openstreetmap"
            ));
            sleepQuietly(1100); // Nominatim usage policy: <= 1 req/sec
            if (hits.size() >= criteria.maxResults()) {
                break;
            }
        }
        return hits;
    }

    private String lookupWebsite(String osmType, String osmId) {
        if (osmId == null || osmId.isBlank()) {
            return null;
        }
        String prefix = switch (osmType == null ? "" : osmType.toUpperCase(Locale.ROOT)) {
            case "N", "NODE" -> "N";
            case "W", "WAY" -> "W";
            case "R", "RELATION" -> "R";
            default -> "N";
        };
        try {
            String url = "https://nominatim.openstreetmap.org/lookup?osm_ids="
                    + prefix + osmId
                    + "&format=json&extratags=1";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return null;
            }
            JsonNode arr = objectMapper.readTree(response.body());
            if (!arr.isArray() || arr.isEmpty()) {
                return null;
            }
            JsonNode extratags = arr.get(0).path("extratags");
            String website = firstNonBlank(
                    text(extratags, "website"),
                    text(extratags, "contact:website"),
                    text(extratags, "url")
            );
            website = WebsiteUrlSupport.normalizeHttpUrl(website);
            return WebsiteUrlSupport.isUsableCompanyWebsite(website) ? website : null;
        } catch (Exception ex) {
            log.debug("Nominatim lookup failed for {}{}: {}", prefix, osmId, ex.getMessage());
            return null;
        }
    }

    private static boolean matchesGeo(
            String featureCity,
            String featureCountry,
            String requestedCity,
            ResolvedDiscoveryCriteria criteria
    ) {
        String cityHay = (featureCity == null ? "" : featureCity).toLowerCase(Locale.ROOT);
        String countryHay = (featureCountry == null ? "" : featureCountry).toLowerCase(Locale.ROOT);
        String combined = cityHay + " " + countryHay;

        if (requestedCity != null && !requestedCity.isBlank()) {
            String req = requestedCity.toLowerCase(Locale.ROOT);
            if (!cityHay.contains(req) && !combined.contains(req)) {
                // Photon sometimes omits city; keep if country matches.
                boolean countryOk = false;
                for (String country : criteria.countryNames()) {
                    if (country != null && countryHay.contains(country.toLowerCase(Locale.ROOT))) {
                        countryOk = true;
                        break;
                    }
                }
                if (!countryOk && !criteria.countryNames().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String firstCountry(ResolvedDiscoveryCriteria criteria) {
        return criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);
    }

    private static List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty()) {
            return List.of("company");
        }
        return values.size() <= max ? values : values.subList(0, max);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private record CityGeo(String cityId, String cityName) {
    }
}
