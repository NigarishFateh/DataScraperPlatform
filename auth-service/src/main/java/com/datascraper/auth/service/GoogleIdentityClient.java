/**
 * Checks Google tokens with Google public APIs.
 */
package com.datascraper.auth.service;

import com.datascraper.auth.config.AuthProperties;
import com.datascraper.auth.domain.GoogleProfile;
import com.datascraper.auth.exception.AuthException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Verifies Google credentials via Google's public endpoints.
 * Never trust the extension alone — always re-verify server-side.
 */
@Service
public class GoogleIdentityClient {

    private final WebClient webClient;
    private final AuthProperties properties;

    public GoogleIdentityClient(WebClient.Builder webClientBuilder, AuthProperties properties) {
        this.webClient = webClientBuilder.build();
        this.properties = properties;
    }

    public GoogleProfile verifyAccessToken(String accessToken) {
        try {
            JsonNode body = webClient.get()
                    .uri(properties.getGoogle().getUserinfoUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return requireProfile(body);
        } catch (WebClientResponseException ex) {
            throw new AuthException("Google access token rejected");
        }
    }

    public GoogleProfile verifyIdToken(String idToken) {
        try {
            String uri = UriComponentsBuilder
                    .fromUriString(properties.getGoogle().getTokeninfoUrl())
                    .queryParam("id_token", idToken)
                    .toUriString();

            JsonNode body = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            GoogleProfile profile = requireProfile(body);

            String audience = text(body, "aud");
            if (!properties.getGoogle().getClientIds().isEmpty()
                    && properties.getGoogle().getClientIds().stream().noneMatch(audience::equals)) {
                throw new AuthException("Google id_token audience is not allowed");
            }

            return profile;
        } catch (WebClientResponseException ex) {
            throw new AuthException("Google id_token rejected");
        }
    }

    private GoogleProfile requireProfile(JsonNode body) {
        if (body == null) {
            throw new AuthException("Empty Google identity response");
        }
        String email = text(body, "email");
        String subject = text(body, "sub");
        if (email.isBlank() || subject.isBlank()) {
            throw new AuthException("Google profile missing email/subject");
        }
        return new GoogleProfile(subject, email, text(body, "name"), text(body, "picture"));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }
}
