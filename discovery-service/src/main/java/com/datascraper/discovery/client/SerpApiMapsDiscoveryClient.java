package com.datascraper.discovery.client;

import com.datascraper.discovery.config.AppProperties;
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
 * SerpAPI Google Maps engine — alternative business search when Google Places key is unavailable.
 */
@Component
public class SerpApiMapsDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(SerpApiMapsDiscoveryClient.class);
    private static final int MAX_CITIES = 500;
    private static final int MAX_KEYWORDS = 2;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private volatile boolean skipFurtherCalls;

    public SerpApiMapsDiscoveryClient(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.apiKey = appProperties.getSerpapiApiKey() == null
                ? ""
                : appProperties.getSerpapiApiKey().trim();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() && !skipFurtherCalls;
    }

    public List<WebSearchHit> discover(ResolvedDiscoveryCriteria criteria) {
        if (!isConfigured()) {
            return List.of();
        }

        if (criteria.hasCompanyNames()) {
            return discoverByCompanyNames(criteria);
        }

        List<WebSearchHit> hits = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<String> keywords = limit(criteria.searchKeywords(), MAX_KEYWORDS);
        String country = criteria.countryNames().isEmpty() ? "" : criteria.countryNames().get(0);

        List<CityGeo> cities = new ArrayList<>();
        int cityLimit = Math.min(criteria.cityNames().size(), MAX_CITIES);
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
                    List<WebSearchHit> batch = searchMaps(query, city, criteria, seen);
                    log.info("SerpAPI Maps '{}' -> {} places", query, batch.size());
                    hits.addAll(batch);
                } catch (Exception ex) {
                    log.warn("SerpAPI Maps search failed for '{}': {}", query, ex.getMessage());
                }
                if (hits.size() >= criteria.maxResults()) {
                    return hits.subList(0, criteria.maxResults());
                }
                sleepQuietly(250);
            }
        }
        return hits;
    }

    /** Custom scrape: one Maps query per company name (no keyword cap). */
    private List<WebSearchHit> discoverByCompanyNames(ResolvedDiscoveryCriteria criteria) {
        List<WebSearchHit> hits = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String country = criteria.countryNames().isEmpty() ? "" : criteria.countryNames().get(0);
        CityGeo location = new CityGeo(null, country.isBlank() ? "Global" : country);

        for (String companyName : criteria.companyNames()) {
            if (companyName == null || companyName.isBlank()) {
                continue;
            }
            String query = (companyName.trim() + " " + country).trim().replaceAll("\\s+", " ");
            try {
                List<WebSearchHit> batch = searchMaps(query, location, criteria, seen);
                log.info("SerpAPI Maps name '{}' -> {} places", query, batch.size());
                hits.addAll(batch);
            } catch (Exception ex) {
                log.warn("SerpAPI Maps name search failed for '{}': {}", query, ex.getMessage());
            }
            if (hits.size() >= criteria.maxResults()) {
                return hits.subList(0, criteria.maxResults());
            }
            sleepQuietly(200);
        }
        return hits;
    }

    private List<WebSearchHit> searchMaps(
            String query,
            CityGeo city,
            ResolvedDiscoveryCriteria criteria,
            Set<String> seen
    ) throws Exception {
        String url = "https://serpapi.com/search.json"
                + "?engine=google_maps"
                + "&type=search"
                + "&hl=en"
                + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(40))
                .header("Accept", "application/json")
                .header("User-Agent", "DataScraperPlatform/0.1")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            if (response.statusCode() == 429 || response.statusCode() == 401 || response.statusCode() == 403) {
                skipFurtherCalls = true;
                log.warn(
                        "SerpAPI Maps disabled for this process (HTTP {}) — {}",
                        response.statusCode(),
                        truncate(response.body(), 160)
                );
            }
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + truncate(response.body(), 240));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode results = root.path("local_results");
        if (!results.isArray() || results.isEmpty()) {
            results = root.path("place_results");
            if (results.isObject()) {
                // Single place payload — wrap as list-like handling below.
                WebSearchHit single = toHit(results, city, criteria, seen);
                return single == null ? List.of() : List.of(single);
            }
            return List.of();
        }

        List<WebSearchHit> hits = new ArrayList<>();
        for (JsonNode item : results) {
            WebSearchHit hit = toHit(item, city, criteria, seen);
            if (hit != null) {
                hits.add(hit);
            }
        }
        return hits;
    }

    private WebSearchHit toHit(
            JsonNode item,
            CityGeo city,
            ResolvedDiscoveryCriteria criteria,
            Set<String> seen
    ) {
        String name = text(item, "title");
        if (name.isBlank()) {
            name = text(item, "name");
        }
        if (name.isBlank()) {
            return null;
        }

        String placeId = text(item, "place_id");
        String dedupe = !placeId.isBlank() ? placeId : name.toLowerCase(Locale.ROOT) + "|" + city.cityName();
        if (!seen.add(dedupe)) {
            return null;
        }

        String website = WebsiteUrlSupport.normalizeHttpUrl(text(item, "website"));
        if (!WebsiteUrlSupport.isUsableCompanyWebsite(website)) {
            // SerpAPI sometimes nests website under "links" or similar keys.
            website = WebsiteUrlSupport.normalizeHttpUrl(text(item.path("links"), "website"));
        }
        if (!WebsiteUrlSupport.isUsableCompanyWebsite(website)) {
            return null;
        }

        String mapsLink = text(item, "link");
        if (mapsLink.isBlank()) {
            mapsLink = text(item.path("gps_coordinates"), "link");
        }
        String sourceUrl = WebsiteUrlSupport.isUsableCompanyWebsite(mapsLink) ? website : mapsLink;
        if (sourceUrl.isBlank() || WebsiteUrlSupport.isMapOrDirectoryUrl(sourceUrl)) {
            sourceUrl = website;
        }

        String address = text(item, "address");
        String resolvedCity = city.cityName();
        if (city.cityName() != null
                && address.toLowerCase(Locale.ROOT).contains(city.cityName().toLowerCase(Locale.ROOT))) {
            resolvedCity = city.cityName();
        }

        return new WebSearchHit(
                name,
                website,
                sourceUrl,
                criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0),
                city.cityId(),
                resolvedCity,
                "serpapi-maps"
        );
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

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
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
