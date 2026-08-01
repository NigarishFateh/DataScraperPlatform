package com.datascraper.discovery.client;

import com.datascraper.discovery.config.AppProperties;
import com.datascraper.discovery.dto.CityDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class LocationCatalogClient {

    private final WebClient webClient;

    public LocationCatalogClient(WebClient.Builder webClientBuilder, AppProperties appProperties) {
        this.webClient = webClientBuilder
                .baseUrl(trimTrailingSlash(appProperties.getLocationServiceUri()))
                .build();
    }

    public List<CityDto> listCitiesByCountry(String countryCode) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/locations/cities")
                        .queryParam("countryCode", countryCode)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CityDto>>() {
                })
                .blockOptional()
                .orElse(List.of());
    }

    public List<String> resolveCityIds(List<String> cityIds, List<String> countryCodes) {
        if (cityIds != null && !cityIds.isEmpty()) {
            return cityIds;
        }
        if (countryCodes == null || countryCodes.isEmpty()) {
            return List.of();
        }

        List<String> resolved = new ArrayList<>();
        for (String countryCode : countryCodes) {
            listCitiesByCountry(countryCode).stream()
                    .map(CityDto::id)
                    .forEach(resolved::add);
        }
        return resolved;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8082";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
