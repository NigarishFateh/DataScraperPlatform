package com.datascraper.common.dto.discovery;

import java.util.List;
import java.util.Map;

/**
 * A company discovered by a DiscoveryProvider — minimal identity before enrichment.
 */
public record DiscoveredCompany(
        String externalId,
        String name,
        String website,
        String countryCode,
        String cityName,
        String cityId,
        List<String> categoryIds,
        String sourceUrl,
        String providerName,
        Map<String, Object> metadata
) {
    public DiscoveredCompany {
        if (categoryIds == null) {
            categoryIds = List.of();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}
