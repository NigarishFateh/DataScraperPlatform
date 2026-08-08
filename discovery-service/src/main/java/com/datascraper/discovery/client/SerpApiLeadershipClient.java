package com.datascraper.discovery.client;

import com.datascraper.discovery.config.AppProperties;
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
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional SerpAPI Google search for "{brand} CEO" style queries.
 * Isolated from Maps-based category discovery.
 */
@Component
public class SerpApiLeadershipClient {

    private static final Logger log = LoggerFactory.getLogger(SerpApiLeadershipClient.class);
    private static final Pattern PERSON_CEO = Pattern.compile(
            "([A-ZÀ-ÖØ-Þ][A-Za-zÀ-öø-ÿ''\\-]+(?:\\s+[A-ZÀ-ÖØ-Þ][A-Za-zÀ-öø-ÿ''\\-]+){1,3})"
                    + ".{0,40}(?i)(ceo|chief executive|managing director|directeur|founder|owner)"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private volatile boolean rateLimited;

    public SerpApiLeadershipClient(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.apiKey = appProperties.getSerpapiApiKey() == null
                ? ""
                : appProperties.getSerpapiApiKey().trim();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() && !rateLimited;
    }

    public Optional<OpenLeadershipClient.Lead> findLeadership(String companyName) {
        if (!isConfigured() || companyName == null || companyName.isBlank()) {
            return Optional.empty();
        }
        try {
            String query = companyName + " CEO OR directeur OR founder";
            String url = "https://serpapi.com/search.json"
                    + "?engine=google"
                    + "&hl=en"
                    + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429 || response.statusCode() == 401 || response.statusCode() == 403) {
                rateLimited = true;
                log.warn(
                        "SerpAPI leadership HTTP {} — skipping further SerpAPI leadership calls this process",
                        response.statusCode()
                );
                return Optional.empty();
            }
            if (response.statusCode() >= 400) {
                log.warn("SerpAPI leadership HTTP {} for {}", response.statusCode(), companyName);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode kg = root.path("knowledge_graph");
            String kgTitle = kg.path("title").asText("").trim();
            String kgType = kg.path("type").asText("").trim();
            if (!kgTitle.isBlank() && kgType.toLowerCase(Locale.ROOT).contains("ceo")) {
                return Optional.of(new OpenLeadershipClient.Lead(
                        kgTitle,
                        "CEO",
                        100,
                        "serpapi"
                ));
            }

            String blob = kg.path("description").asText("");
            for (JsonNode result : root.path("organic_results")) {
                blob = blob + " " + result.path("title").asText("") + " " + result.path("snippet").asText("");
            }
            Matcher matcher = PERSON_CEO.matcher(blob);
            if (matcher.find()) {
                String name = matcher.group(1).trim();
                String title = OpenLeadershipClient.normalizeTitlePublic(matcher.group(2));
                return Optional.of(new OpenLeadershipClient.Lead(
                        name,
                        title,
                        OpenLeadershipClient.scoreTitle(title),
                        "serpapi"
                ));
            }
        } catch (Exception ex) {
            log.warn("SerpAPI leadership failed for {}: {}", companyName, ex.getMessage());
        }
        return Optional.empty();
    }
}
