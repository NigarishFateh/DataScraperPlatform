package com.datascraper.discovery.client;

import com.datascraper.discovery.config.AppProperties;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Optional Apollo People lookup for leadership. Isolated from company discovery.
 * Free plans often return 403 for people search — callers must treat empty as normal.
 */
@Component
public class ApolloPeopleLeadershipClient {

    private static final Logger log = LoggerFactory.getLogger(ApolloPeopleLeadershipClient.class);
    private static final String SEARCH_URL = "https://api.apollo.io/api/v1/mixed_people/api_search";

    private static final List<String> LEADERSHIP_TITLES = List.of(
            "CEO",
            "Chief Executive Officer",
            "Owner",
            "Founder",
            "Co-Founder",
            "Managing Director",
            "General Manager",
            "Director",
            "Restaurant Manager",
            "Store Manager",
            "Franchise Owner",
            "Manager",
            "Directeur",
            "Algemeen Directeur",
            "Eigenaar",
            "Zaakvoerder",
            "Bedrijfsleider"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private volatile boolean peopleApiBlocked;

    public ApolloPeopleLeadershipClient(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.apiKey = appProperties.getApolloApiKey() == null
                ? ""
                : appProperties.getApolloApiKey().trim();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() && !peopleApiBlocked;
    }

    public Optional<Lead> findLeadership(String companyDomain, String organizationLocation) {
        if (!isConfigured() || companyDomain == null || companyDomain.isBlank()) {
            return Optional.empty();
        }
        String domain = stripToDomain(companyDomain);
        if (domain == null || domain.isBlank()) {
            return Optional.empty();
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode domains = body.putArray("q_organization_domains_list");
            domains.add(domain);
            ArrayNode titles = body.putArray("person_titles");
            for (String title : LEADERSHIP_TITLES) {
                titles.add(title);
            }
            if (organizationLocation != null && !organizationLocation.isBlank()) {
                ArrayNode locations = body.putArray("organization_locations");
                locations.add(organizationLocation.trim());
            }
            body.put("include_similar_titles", true);
            body.put("page", 1);
            body.put("per_page", 10);

            HttpRequest request = HttpRequest.newBuilder(URI.create(SEARCH_URL))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .header("x-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                peopleApiBlocked = true;
                log.info(
                        "Apollo people search unavailable (HTTP {}) — skipping further people calls this process",
                        response.statusCode()
                );
                return Optional.empty();
            }
            if (response.statusCode() >= 400) {
                log.warn("Apollo people search HTTP {} for domain {}", response.statusCode(), domain);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode people = root.path("people");
            if (!people.isArray() || people.isEmpty()) {
                people = root.path("contacts");
            }
            if (!people.isArray() || people.isEmpty()) {
                return Optional.empty();
            }

            List<Lead> leads = new ArrayList<>();
            for (JsonNode person : people) {
                toLead(person).ifPresent(leads::add);
            }
            return leads.stream().max(Comparator.comparingInt(Lead::score));
        } catch (Exception ex) {
            log.warn("Apollo people search failed for {}: {}", domain, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Lead> toLead(JsonNode person) {
        String first = text(person, "first_name");
        String last = text(person, "last_name");
        if (last.isBlank()) {
            last = text(person, "last_name_obfuscated").replace("*", "").trim();
        }
        String name = (first + " " + last).trim();
        if (name.isBlank() || name.length() < 3) {
            return Optional.empty();
        }
        String title = text(person, "title");
        if (title.isBlank()) {
            title = text(person, "headline");
        }
        return Optional.of(new Lead(name, title, scoreTitle(title), "apollo-people"));
    }

    static int scoreTitle(String title) {
        if (title == null || title.isBlank()) {
            return 1;
        }
        String t = title.toLowerCase(Locale.ROOT);
        if (t.contains("ceo") || t.contains("chief executive")) {
            return 100;
        }
        if (t.contains("managing director") || t.contains("algemeen directeur") || t.contains("directeur")) {
            return 90;
        }
        if (t.contains("founder") || t.contains("owner") || t.contains("eigenaar") || t.contains("zaakvoerder")) {
            return 85;
        }
        if (t.contains("general manager") || t.contains("bedrijfsleider")) {
            return 70;
        }
        if (t.contains("director") || t.contains("manager")) {
            return 50;
        }
        return 10;
    }

    private static String stripToDomain(String website) {
        if (website == null || website.isBlank()) {
            return null;
        }
        String value = website.trim().toLowerCase(Locale.ROOT);
        value = value.replaceFirst("^https?://", "").replaceFirst("^www\\.", "");
        int slash = value.indexOf('/');
        if (slash >= 0) {
            value = value.substring(0, slash);
        }
        return value.isBlank() ? null : value;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    public record Lead(String name, String title, int score, String source) {
    }
}
