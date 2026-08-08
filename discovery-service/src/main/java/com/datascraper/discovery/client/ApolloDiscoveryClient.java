package com.datascraper.discovery.client;

import com.datascraper.discovery.config.AppProperties;
import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import com.datascraper.discovery.dto.WebSearchHit;
import com.datascraper.discovery.support.WebsiteUrlSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
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
 * Apollo.io Organization Search — primary B2B company discovery by city/country.
 * Uses /api/v1/organizations/search (works on Free when that endpoint is enabled).
 * Do NOT use /mixed_companies/search — Free plan returns API_INACCESSIBLE (HTTP 403).
 */
@Component
public class ApolloDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(ApolloDiscoveryClient.class);
    private static final String SEARCH_URL = "https://api.apollo.io/api/v1/organizations/search";
    private static final int MAX_CITIES = 500;
    private static final int MAX_KEYWORDS = 3;
    private static final int PER_PAGE = 100;
    private static final int MAX_PAGES_PER_QUERY = 100;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public ApolloDiscoveryClient(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.apiKey = appProperties.getApolloApiKey() == null
                ? ""
                : appProperties.getApolloApiKey().trim();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public List<WebSearchHit> discover(ResolvedDiscoveryCriteria criteria) {
        if (!isConfigured()) {
            return List.of();
        }

        List<WebSearchHit> hits = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<String> keywords = limit(criteria.searchKeywords(), MAX_KEYWORDS);
        String country = criteria.countryNames().isEmpty() ? "" : criteria.countryNames().get(0);

        List<CityGeo> cities = buildCities(criteria, country);
        for (CityGeo city : cities) {
            String location = buildLocation(city.cityName(), country);
            for (String keyword : keywords) {
                if (keyword == null || keyword.isBlank()) {
                    continue;
                }
                try {
                    List<WebSearchHit> batch = searchCityKeyword(location, keyword, city, criteria, seen);
                    log.info("Apollo '{}' / '{}' -> {} orgs", location, keyword, batch.size());
                    hits.addAll(batch);
                } catch (Exception ex) {
                    log.warn("Apollo search failed for '{}' / '{}': {}", location, keyword, ex.getMessage());
                }
                if (hits.size() >= criteria.maxResults()) {
                    return hits.subList(0, criteria.maxResults());
                }
                sleepQuietly(200);
            }
        }
        return hits;
    }

    private List<WebSearchHit> searchCityKeyword(
            String location,
            String keyword,
            CityGeo city,
            ResolvedDiscoveryCriteria criteria,
            Set<String> seen
    ) throws Exception {
        List<WebSearchHit> hits = new ArrayList<>();
        int maxPages = Math.min(
                MAX_PAGES_PER_QUERY,
                Math.max(1, (int) Math.ceil((double) Math.max(1, criteria.maxResults()) / PER_PAGE))
        );

        for (int page = 1; page <= maxPages; page++) {
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode locations = body.putArray("organization_locations");
            locations.add(location);

            ArrayNode tags = body.putArray("q_organization_keyword_tags");
            tags.add(keyword.trim());

            body.put("page", page);
            body.put("per_page", PER_PAGE);

            HttpRequest request = HttpRequest.newBuilder(URI.create(SEARCH_URL))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .header("x-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException(
                        "HTTP " + response.statusCode() + ": " + truncate(response.body(), 280)
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode orgs = root.path("organizations");
            if (!orgs.isArray() || orgs.isEmpty()) {
                orgs = root.path("companies");
            }
            if (!orgs.isArray() || orgs.isEmpty()) {
                break;
            }

            for (JsonNode org : orgs) {
                WebSearchHit hit = toHit(org, city, criteria, seen);
                if (hit != null) {
                    hits.add(hit);
                }
                if (hits.size() >= criteria.maxResults()) {
                    return hits;
                }
            }

            JsonNode pagination = root.path("pagination");
            int totalPages = pagination.path("total_pages").asInt(page);
            if (page >= totalPages || page >= MAX_PAGES_PER_QUERY) {
                break;
            }
            sleepQuietly(150);
        }
        return hits;
    }

    private WebSearchHit toHit(
            JsonNode org,
            CityGeo city,
            ResolvedDiscoveryCriteria criteria,
            Set<String> seen
    ) {
        String name = text(org, "name");
        if (name.isBlank()) {
            return null;
        }

        String id = text(org, "id");
        String dedupe = !id.isBlank() ? id : name.toLowerCase(Locale.ROOT) + "|" + city.cityName();
        if (!seen.add(dedupe)) {
            return null;
        }

        String website = WebsiteUrlSupport.normalizeHttpUrl(text(org, "website_url"));
        if (!WebsiteUrlSupport.isUsableCompanyWebsite(website)) {
            String domain = text(org, "primary_domain");
            if (!domain.isBlank()) {
                website = WebsiteUrlSupport.normalizeHttpUrl(
                        domain.startsWith("http") ? domain : "https://" + domain
                );
            }
        }
        if (!WebsiteUrlSupport.isUsableCompanyWebsite(website)) {
            return null;
        }

        String resolvedCity = text(org, "city");
        if (resolvedCity.isBlank()) {
            resolvedCity = city.cityName();
        }

        String countryCode = criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);
        String orgCountry = text(org, "country");
        if (!orgCountry.isBlank() && countryCode == null) {
            countryCode = orgCountry;
        }

        return new WebSearchHit(
                name,
                website,
                website,
                countryCode,
                city.cityId(),
                resolvedCity,
                "apollo"
        );
    }

    private static String buildLocation(String city, String country) {
        if (city == null || city.isBlank()) {
            return country == null ? "" : country.trim();
        }
        if (country == null || country.isBlank() || city.equalsIgnoreCase(country)) {
            return city.trim();
        }
        return city.trim() + ", " + country.trim();
    }

    private static List<CityGeo> buildCities(ResolvedDiscoveryCriteria criteria, String country) {
        List<CityGeo> cities = new ArrayList<>();
        int limit = Math.min(criteria.cityNames().size(), MAX_CITIES);
        for (int i = 0; i < limit; i++) {
            String cityName = criteria.cityNames().get(i);
            if (cityName == null || cityName.isBlank()) {
                continue;
            }
            String cityId = i < criteria.cityIds().size() ? criteria.cityIds().get(i) : null;
            cities.add(new CityGeo(cityId, cityName));
        }
        if (cities.isEmpty() && country != null && !country.isBlank()) {
            cities.add(new CityGeo(null, country));
        }
        return cities;
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
