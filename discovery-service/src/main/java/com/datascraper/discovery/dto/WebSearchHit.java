package com.datascraper.discovery.dto;

/**
 * A single web/search hit that may represent a company.
 */
public record WebSearchHit(
        String name,
        String website,
        String sourceUrl,
        String countryCode,
        String cityId,
        String cityName,
        String providerSource
) {
}
