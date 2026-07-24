/**
 * Calls the GitHub API to search for matching organizations.
 */
package com.datascraper.github.client;

import com.datascraper.github.config.GitHubScraperProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GitHubSearchClient {

    private final WebClient gitHubWebClient;
    private final GitHubScraperProperties properties;

    public GitHubSearchClient(WebClient gitHubWebClient, GitHubScraperProperties properties) {
        this.gitHubWebClient = gitHubWebClient;
        this.properties = properties;
    }

    public List<Map<String, Object>> searchOrganizations(String companyName) {
        String query = companyName.trim() + " in:login type:org";

        JsonNode response;
        try {
            response = gitHubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/users")
                            .queryParam("q", query)
                            .queryParam("per_page", properties.getMaxResults())
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException("GitHub API error: " + ex.getStatusCode().value());
        }

        if (response == null || !response.has("items")) {
            return List.of();
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (JsonNode item : response.get("items")) {
            Map<String, Object> org = new LinkedHashMap<>();
            org.put("section", "presence");
            org.put("field", "github-organization");
            org.put("login", item.path("login").asText());
            org.put("profileUrl", item.path("html_url").asText());
            org.put("avatarUrl", item.path("avatar_url").asText());
            items.add(org);
        }
        return items;
    }
}
