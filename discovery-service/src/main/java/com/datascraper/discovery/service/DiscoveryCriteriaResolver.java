package com.datascraper.discovery.service;

import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.discovery.client.CategoryCatalogClient;
import com.datascraper.discovery.client.LocationCatalogClient;
import com.datascraper.discovery.dto.CategoryDto;
import com.datascraper.discovery.dto.CityDto;
import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DiscoveryCriteriaResolver {

    private static final Map<String, String> COUNTRY_NAMES = Map.ofEntries(
            Map.entry("PK", "Pakistan"),
            Map.entry("US", "United States"),
            Map.entry("GB", "United Kingdom"),
            Map.entry("IN", "India"),
            Map.entry("DE", "Germany"),
            Map.entry("AE", "United Arab Emirates"),
            Map.entry("SA", "Saudi Arabia"),
            Map.entry("CA", "Canada"),
            Map.entry("AU", "Australia"),
            Map.entry("FR", "France"),
            Map.entry("NL", "Netherlands"),
            Map.entry("SG", "Singapore")
    );

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.ofEntries(
            Map.entry("ai", List.of("artificial intelligence", "AI", "machine learning", "deep learning", "LLM")),
            Map.entry("ml", List.of("machine learning", "ML", "data science", "AI")),
            Map.entry("software", List.of("software", "SaaS", "IT services", "software development")),
            Map.entry("software-dev", List.of("software development", "custom software", "IT company")),
            Map.entry("cybersecurity", List.of("cybersecurity", "infoSec", "security software")),
            Map.entry("fintech", List.of("fintech", "financial technology", "payments")),
            Map.entry("cleaning", List.of("cleaning company", "janitorial", "facility cleaning")),
            Map.entry("automation", List.of("automation", "RPA", "industrial automation"))
    );

    private final CategoryCatalogClient categoryCatalogClient;
    private final LocationCatalogClient locationCatalogClient;

    public DiscoveryCriteriaResolver(
            CategoryCatalogClient categoryCatalogClient,
            LocationCatalogClient locationCatalogClient
    ) {
        this.categoryCatalogClient = categoryCatalogClient;
        this.locationCatalogClient = locationCatalogClient;
    }

    public ResolvedDiscoveryCriteria resolve(DiscoveryRequest request) {
        List<CategoryDto> categories = categoryCatalogClient.listByIds(request.categoryIds());
        List<String> categoryNames = categories.stream()
                .map(CategoryDto::name)
                .filter(name -> name != null && !name.isBlank())
                .toList();
        if (categoryNames.isEmpty() && request.categoryIds() != null) {
            categoryNames = request.categoryIds();
        }

        List<String> countryCodes = request.countryCodes() == null ? List.of() : request.countryCodes();
        List<String> countryNames = new ArrayList<>();
        for (String code : countryCodes) {
            String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
            countryNames.add(COUNTRY_NAMES.getOrDefault(normalized, normalized));
        }

        List<String> cityIds = request.cityIds() == null ? List.of() : request.cityIds();
        List<String> cityNames = new ArrayList<>();
        // Only resolve explicitly selected cities. Empty city list + country = nationwide scrape.
        if (!cityIds.isEmpty()) {
            for (String cityId : cityIds) {
                CityDto city = locationCatalogClient.findCityById(cityId);
                if (city != null && city.name() != null && !city.name().isBlank()) {
                    cityNames.add(city.name());
                } else if (cityId != null && cityId.contains("-")) {
                    cityNames.add(humanizeCityId(cityId));
                }
            }
        }

        Set<String> keywords = new LinkedHashSet<>();
        for (String categoryId : request.categoryIds() == null ? List.<String>of() : request.categoryIds()) {
            List<String> mapped = CATEGORY_KEYWORDS.get(categoryId == null ? "" : categoryId.toLowerCase(Locale.ROOT));
            if (mapped != null) {
                keywords.addAll(mapped);
            }
        }
        keywords.addAll(categoryNames);
        if (keywords.isEmpty()) {
            keywords.add("company");
        }

        return new ResolvedDiscoveryCriteria(
                request.categoryIds() == null ? List.of() : request.categoryIds(),
                categoryNames,
                countryCodes,
                countryNames,
                cityIds,
                cityNames.stream().distinct().toList(),
                List.copyOf(keywords),
                request.maxResults()
        );
    }

    private static String humanizeCityId(String cityId) {
        String[] parts = cityId.split("-", 2);
        String raw = parts.length == 2 ? parts[1] : cityId;
        if (raw.isBlank()) {
            return cityId;
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase(Locale.ROOT);
    }
}
