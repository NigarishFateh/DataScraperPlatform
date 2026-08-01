package com.datascraper.common.dto.company;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Fully enriched, normalized company record ready for persistence and export.
 * Unavailable fields remain null/empty — never fabricated.
 */
public record EnrichedCompany(
        String id,
        String name,
        String category,
        String industry,
        String countryCode,
        String countryName,
        String state,
        String city,
        String website,
        String email,
        String phone,
        String founder,
        String ceo,
        String description,
        String services,
        String products,
        List<String> technologyStack,
        String linkedIn,
        String github,
        String facebook,
        String twitter,
        String instagram,
        String youtube,
        Integer foundedYear,
        String employeeCount,
        String address,
        String contactPage,
        String sourceUrl,
        Instant scrapedAt,
        double confidenceScore,
        String providerName,
        String notes,
        List<String> categoryIds,
        Map<String, Object> rawAttributes
) {
    public EnrichedCompany {
        if (technologyStack == null) {
            technologyStack = List.of();
        }
        if (categoryIds == null) {
            categoryIds = List.of();
        }
        if (rawAttributes == null) {
            rawAttributes = Map.of();
        }
    }
}
