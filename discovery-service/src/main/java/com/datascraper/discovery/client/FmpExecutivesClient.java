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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Financial Modeling Prep Company Executives API.
 * Free tier: ~250 req/day; best for publicly traded symbols (MCD, QSR, YUM, DPZ, …).
 * Isolated from category discovery.
 */
@Component
public class FmpExecutivesClient {

    private static final Logger log = LoggerFactory.getLogger(FmpExecutivesClient.class);
    private static final String STABLE_URL = "https://financialmodelingprep.com/stable/key-executives";
    private static final String V3_URL_PREFIX = "https://financialmodelingprep.com/api/v3/key-executives/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public FmpExecutivesClient(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.apiKey = appProperties.getFmpApiKey() == null
                ? ""
                : appProperties.getFmpApiKey().trim();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    /**
     * Returns the best CEO/director/manager-like executive for a stock symbol.
     *
     * @param symbol    ticker (e.g. MCD, YUM)
     * @param brandHint optional brand name used to prefer division-specific titles
     */
    public Optional<ExecutiveLead> findLeadership(String symbol, String brandHint) {
        if (!isConfigured() || symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }
        String ticker = symbol.trim().toUpperCase(Locale.ROOT);
        try {
            JsonNode root = fetchExecutives(ticker);
            if (root == null || !root.isArray() || root.isEmpty()) {
                return Optional.empty();
            }

            List<ExecutiveLead> leads = new ArrayList<>();
            for (JsonNode node : root) {
                toLead(node, ticker, brandHint).ifPresent(leads::add);
            }
            Optional<ExecutiveLead> best = leads.stream().max(Comparator.comparingInt(ExecutiveLead::score));
            best.ifPresent(lead -> log.info(
                    "FMP {} -> {} ({}) pay={}",
                    ticker,
                    lead.name(),
                    lead.title(),
                    lead.compensation() == null ? "n/a" : lead.compensation()
            ));
            return best;
        } catch (Exception ex) {
            log.warn("FMP executives failed for {}: {}", ticker, ex.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode fetchExecutives(String ticker) throws Exception {
        String stable = STABLE_URL
                + "?symbol=" + URLEncoder.encode(ticker, StandardCharsets.UTF_8)
                + "&apikey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        JsonNode root = getJson(stable);
        if (root != null && root.isArray() && !root.isEmpty()) {
            return root;
        }
        String v3 = V3_URL_PREFIX + URLEncoder.encode(ticker, StandardCharsets.UTF_8)
                + "?apikey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        return getJson(v3);
    }

    private Optional<ExecutiveLead> toLead(JsonNode node, String ticker, String brandHint) {
        String name = text(node, "name");
        if (name.isBlank()) {
            name = text(node, "executiveName");
        }
        if (name.isBlank() || name.length() < 3) {
            return Optional.empty();
        }
        String title = text(node, "title");
        if (title.isBlank()) {
            title = text(node, "position");
        }
        Long pay = null;
        if (node.path("pay").isNumber()) {
            pay = node.path("pay").asLong();
        } else if (node.path("compensation").isNumber()) {
            pay = node.path("compensation").asLong();
        }
        int score = scoreExecutive(title, brandHint);
        if (score < 20) {
            return Optional.empty();
        }
        return Optional.of(new ExecutiveLead(name.trim(), title.trim(), score, ticker, pay, "fmp"));
    }

    static int scoreExecutive(String title, String brandHint) {
        if (title == null || title.isBlank()) {
            return 15;
        }
        String t = title.toLowerCase(Locale.ROOT);
        int score = ApolloPeopleLeadershipClient.scoreTitle(title);
        if (t.contains("chief executive") || t.equals("ceo") || t.startsWith("ceo ") || t.contains(" ceo")) {
            score = Math.max(score, 100);
        }
        if (brandHint != null && !brandHint.isBlank()) {
            String hint = brandHint.toLowerCase(Locale.ROOT);
            // Prefer division-specific titles for YUM brands (KFC / Taco Bell / Pizza Hut).
            if (hint.contains("kfc") && t.contains("kfc")) {
                score += 25;
            }
            if (hint.contains("taco") && t.contains("taco")) {
                score += 25;
            }
            if (hint.contains("pizza hut") && t.contains("pizza hut")) {
                score += 25;
            }
            if (hint.contains("burger") && (t.contains("burger") || t.contains("restaurant brands"))) {
                score += 15;
            }
        }
        if (t.contains("independent") || t.contains("director only") || t.contains("non-executive")) {
            score -= 30;
        }
        return score;
    }

    private JsonNode getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            log.warn("FMP unauthorized HTTP {} — check FMP_API_KEY / plan", response.statusCode());
            return null;
        }
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode() + ": "
                    + truncate(response.body(), 180));
        }
        return objectMapper.readTree(response.body());
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record ExecutiveLead(
            String name,
            String title,
            int score,
            String ticker,
            Long compensation,
            String source
    ) {
    }
}
