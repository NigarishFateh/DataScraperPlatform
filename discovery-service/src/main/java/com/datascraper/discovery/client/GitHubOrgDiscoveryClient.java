package com.datascraper.discovery.client;

import com.datascraper.discovery.config.AppProperties;
import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import com.datascraper.discovery.dto.WebSearchHit;
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
import java.util.List;
import java.util.Locale;

/**
 * Discovers organizations via GitHub user/org search filtered by location keywords.
 */
@Component
public class GitHubOrgDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubOrgDiscoveryClient.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper objectMapper;
    private final String githubToken;

    public GitHubOrgDiscoveryClient(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.githubToken = appProperties.getGithubToken() == null ? "" : appProperties.getGithubToken().trim();
    }

    public List<WebSearchHit> discover(ResolvedDiscoveryCriteria criteria) {
        List<String> locations = buildLocationQueries(criteria);
        if (locations.isEmpty()) {
            locations = List.of("");
        }

        List<WebSearchHit> hits = new ArrayList<>();
        for (String location : locations) {
            List<String> keywords = new ArrayList<>(limit(criteria.searchKeywords(), 3));
            keywords.add(""); // location-only fallback expands recall for niche categories
            for (String keyword : keywords) {
                String query = buildQuery(location, keyword);
                try {
                    hits.addAll(search(query, criteria, Math.min(30, criteria.maxResults())));
                } catch (Exception ex) {
                    log.warn("GitHub discovery failed for query '{}': {}", query, ex.getMessage());
                }
                if (hits.size() >= criteria.maxResults()) {
                    return hits;
                }
                sleepQuietly(350);
            }
        }
        return hits;
    }

    private List<WebSearchHit> search(
            String query,
            ResolvedDiscoveryCriteria criteria,
            int perPage
    ) throws Exception {
        String url = "https://api.github.com/search/users?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&per_page=" + Math.max(1, Math.min(perPage, 30));

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "DataScraperPlatform/0.1")
                .GET();
        if (!githubToken.isBlank()) {
            builder.header("Authorization", "Bearer " + githubToken);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode items = root.path("items");
        List<WebSearchHit> hits = new ArrayList<>();
        if (!items.isArray()) {
            return hits;
        }

        for (JsonNode item : items) {
            String login = text(item, "login");
            String apiUrl = text(item, "url");
            if (apiUrl.isBlank()) {
                continue;
            }
            JsonNode details = fetchUser(apiUrl);
            String name = firstNonBlank(text(details, "name"), login);
            String blog = normalizeWebsite(text(details, "blog"));
            String htmlUrl = text(details, "html_url");
            String location = text(details, "location");
            if (isExcludedOrg(name) || isExcludedOrg(login)) {
                continue;
            }
            if (!matchesLocation(location, criteria)) {
                continue;
            }
            String website = blog.isBlank() ? htmlUrl : blog;
            if (website.isBlank()) {
                continue;
            }
            hits.add(new WebSearchHit(
                    name,
                    website,
                    htmlUrl.isBlank() ? website : htmlUrl,
                    firstCountry(criteria),
                    firstCityId(criteria),
                    firstCityName(criteria),
                    "github"
            ));
            sleepQuietly(200);
        }
        return hits;
    }

    private JsonNode fetchUser(String apiUrl) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "DataScraperPlatform/0.1")
                .GET();
        if (!githubToken.isBlank()) {
            builder.header("Authorization", "Bearer " + githubToken);
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(response.body());
    }

    private static String buildQuery(String location, String keyword) {
        StringBuilder query = new StringBuilder("type:org");
        if (location != null && !location.isBlank()) {
            query.append(" location:").append(quoteIfNeeded(location));
        }
        if (keyword != null && !keyword.isBlank()) {
            query.append(' ').append(keyword);
        }
        return query.toString();
    }

    private static List<String> buildLocationQueries(ResolvedDiscoveryCriteria criteria) {
        List<String> locations = new ArrayList<>();
        for (String city : criteria.cityNames()) {
            if (city != null && !city.isBlank()) {
                locations.add(city);
                if (!criteria.countryNames().isEmpty()) {
                    locations.add(city + ", " + criteria.countryNames().get(0));
                }
            }
        }
        if (locations.isEmpty()) {
            locations.addAll(criteria.countryNames());
        }
        return locations.stream().distinct().toList();
    }

    private static boolean matchesLocation(String location, ResolvedDiscoveryCriteria criteria) {
        if (location == null || location.isBlank()) {
            return criteria.cityNames().isEmpty() && criteria.countryNames().isEmpty();
        }
        String normalized = location.toLowerCase(Locale.ROOT);
        for (String city : criteria.cityNames()) {
            if (city != null && !city.isBlank() && normalized.contains(city.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        for (String country : criteria.countryNames()) {
            if (country != null && !country.isBlank() && normalized.contains(country.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        for (String code : criteria.countryCodes()) {
            if (code != null && !code.isBlank() && normalized.contains(code.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return criteria.cityNames().isEmpty() && criteria.countryNames().isEmpty();
    }

    private static boolean isExcludedOrg(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("university")
                || lower.contains("student")
                || lower.contains("chapter")
                || lower.contains("community")
                || lower.contains("gdg")
                || lower.contains("mlsa")
                || lower.contains("bootcamp")
                || lower.contains("meetup");
    }

    private static String normalizeWebsite(String website) {
        if (website == null || website.isBlank()) {
            return "";
        }
        String value = website.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return "https://" + value;
    }

    private static String quoteIfNeeded(String value) {
        if (value.contains(" ") || value.contains(",")) {
            return "\"" + value + "\"";
        }
        return value;
    }

    private static String firstCountry(ResolvedDiscoveryCriteria criteria) {
        return criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);
    }

    private static String firstCityId(ResolvedDiscoveryCriteria criteria) {
        return criteria.cityIds().isEmpty() ? null : criteria.cityIds().get(0);
    }

    private static String firstCityName(ResolvedDiscoveryCriteria criteria) {
        return criteria.cityNames().isEmpty() ? null : criteria.cityNames().get(0);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private static List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty()) {
            return List.of("company");
        }
        return values.size() <= max ? values : values.subList(0, max);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
