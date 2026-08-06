package com.datascraper.discovery.client;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.discovery.config.AppProperties;
import com.datascraper.discovery.dto.CityDto;
import com.datascraper.discovery.dto.CountryDto;
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

    public String findCountryName(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        String code = countryCode.trim().toUpperCase();
        PageResponse<CountryDto> page = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/locations/countries")
                        .queryParam("search", code)
                        .queryParam("page", 0)
                        .queryParam("pageSize", 50)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PageResponse<CountryDto>>() {
                })
                .blockOptional()
                .orElse(null);
        if (page == null || page.items() == null) {
            return null;
        }
        return page.items().stream()
                .filter(country -> country != null && code.equalsIgnoreCase(country.code()))
                .map(CountryDto::name)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }

    public CityDto findCityById(String cityId) {
        if (cityId == null || cityId.isBlank()) {
            return null;
        }
        String countryGuess = null;
        int dash = cityId.indexOf('-');
        if (dash > 0) {
            countryGuess = cityId.substring(0, dash).toUpperCase();
        }
        List<CityDto> cities = countryGuess == null
                ? searchCities(cityId)
                : listCitiesByCountry(countryGuess);
        return cities.stream()
                .filter(city -> cityId.equalsIgnoreCase(city.id()))
                .findFirst()
                .orElseGet(() -> searchCities(cityId).stream()
                        .filter(city -> cityId.equalsIgnoreCase(city.id()))
                        .findFirst()
                        .orElse(null));
    }

    public List<CityDto> searchCities(String search) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/locations/cities")
                        .queryParam("search", search == null ? "" : search)
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
