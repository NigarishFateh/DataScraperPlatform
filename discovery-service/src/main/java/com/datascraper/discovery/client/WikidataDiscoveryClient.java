package com.datascraper.discovery.client;

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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Discovers companies from Wikidata (country + industry/keyword heuristics).
 */
@Component
public class WikidataDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(WikidataDiscoveryClient.class);

    private static final Map<String, String> COUNTRY_QIDS = Map.ofEntries(
            Map.entry("PK", "Q843"),
            Map.entry("US", "Q30"),
            Map.entry("GB", "Q145"),
            Map.entry("IN", "Q668"),
            Map.entry("DE", "Q183"),
            Map.entry("AE", "Q878"),
            Map.entry("SA", "Q851"),
            Map.entry("CA", "Q16"),
            Map.entry("AU", "Q408"),
            Map.entry("FR", "Q142"),
            Map.entry("NL", "Q55"),
            Map.entry("SG", "Q334")
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final ObjectMapper objectMapper;

    public WikidataDiscoveryClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<WebSearchHit> discover(ResolvedDiscoveryCriteria criteria) {
        if (criteria.countryCodes().isEmpty()) {
            return List.of();
        }

        List<WebSearchHit> hits = new ArrayList<>();
        for (String countryCode : criteria.countryCodes()) {
            String qid = COUNTRY_QIDS.get(countryCode.trim().toUpperCase(Locale.ROOT));
            if (qid == null) {
                continue;
            }
            try {
                hits.addAll(queryCountry(qid, countryCode, criteria));
            } catch (Exception ex) {
                log.warn("Wikidata discovery failed for {}: {}", countryCode, ex.getMessage());
            }
        }
        return hits;
    }

    private List<WebSearchHit> queryCountry(
            String countryQid,
            String countryCode,
            ResolvedDiscoveryCriteria criteria
    ) throws Exception {
        String regex = buildKeywordRegex(criteria.searchKeywords());
        String cityFilter = "";
        if (!criteria.cityNames().isEmpty()) {
            String cityRegex = criteria.cityNames().stream()
                    .map(WikidataDiscoveryClient::escapeRegex)
                    .collect(Collectors.joining("|"));
            cityFilter = """
                    FILTER(BOUND(?cityLabel) && REGEX(LCASE(?cityLabel), "%s"))
                    """.formatted(cityRegex.toLowerCase(Locale.ROOT));
        }

        String sparql = """
                SELECT DISTINCT ?itemLabel ?website ?cityLabel WHERE {
                  ?item wdt:P31/wdt:P279* wd:Q4830453 .
                  ?item wdt:P17 wd:%s .
                  {
                    ?item wdt:P452/rdfs:label ?indLabel . FILTER(LANG(?indLabel)="en")
                    FILTER(REGEX(LCASE(?indLabel), "%s"))
                  } UNION {
                    ?item rdfs:label ?name . FILTER(LANG(?name)="en")
                    FILTER(REGEX(LCASE(?name), "%s"))
                  }
                  OPTIONAL { ?item wdt:P856 ?website }
                  OPTIONAL {
                    ?item wdt:P159 ?city .
                    ?city rdfs:label ?cityLabel . FILTER(LANG(?cityLabel)="en")
                  }
                  %s
                  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
                }
                LIMIT %d
                """.formatted(
                countryQid,
                regex,
                regex,
                cityFilter,
                Math.max(10, Math.min(criteria.maxResults(), 80))
        );

        String url = "https://query.wikidata.org/sparql?format=json&query="
                + URLEncoder.encode(sparql, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(90))
                .header("User-Agent", "DataScraperPlatform/0.1 (local-dev)")
                .header("Accept", "application/sparql-results+json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        JsonNode bindings = objectMapper.readTree(response.body()).path("results").path("bindings");
        List<WebSearchHit> hits = new ArrayList<>();
        if (!bindings.isArray()) {
            return hits;
        }

        for (JsonNode row : bindings) {
            String name = text(row, "itemLabel");
            String website = text(row, "website");
            String cityName = text(row, "cityLabel");
            if (name.isBlank() || website.isBlank()) {
                continue;
            }
            if (isExcluded(name)) {
                continue;
            }
            hits.add(new WebSearchHit(
                    name,
                    website,
                    website,
                    countryCode,
                    firstCityId(criteria),
                    cityName.isBlank() ? firstCityName(criteria) : cityName,
                    "wikidata"
            ));
        }
        return hits;
    }

    private static String buildKeywordRegex(List<String> keywords) {
        List<String> parts = new ArrayList<>();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            String lower = keyword.toLowerCase(Locale.ROOT);
            if (lower.equals("ai")) {
                parts.add("\\bai\\b");
                parts.add("artificial");
                parts.add("machine learning");
                continue;
            }
            parts.add(escapeRegex(lower));
        }
        if (parts.isEmpty()) {
            return "software|technolog|digital|systems|data|cyber|artificial|intelligence";
        }
        return String.join("|", parts);
    }

    private static String escapeRegex(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("+", "\\+")
                .replace("*", "\\*")
                .replace("?", "\\?")
                .replace("|", "\\|");
    }

    private static boolean isExcluded(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("university")
                || lower.contains("college")
                || lower.contains("school")
                || lower.contains("department")
                || lower.contains("ministry")
                || lower.contains("hotel");
    }

    private static String text(JsonNode row, String field) {
        JsonNode value = row.path(field).path("value");
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private static String firstCityId(ResolvedDiscoveryCriteria criteria) {
        return criteria.cityIds().isEmpty() ? null : criteria.cityIds().get(0);
    }

    private static String firstCityName(ResolvedDiscoveryCriteria criteria) {
        return criteria.cityNames().isEmpty() ? null : criteria.cityNames().get(0);
    }
}
