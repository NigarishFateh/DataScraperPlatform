package com.datascraper.discovery.dto;

/**
 * A single web/search hit that may represent a company or branch location.
 */
public record WebSearchHit(
        String name,
        String website,
        String sourceUrl,
        String countryCode,
        String cityId,
        String cityName,
        String providerSource,
        String address,
        String phone,
        String placeId
) {
    /**
     * Compatibility constructor for callers that do not yet populate location fields.
     */
    public WebSearchHit(
            String name,
            String website,
            String sourceUrl,
            String countryCode,
            String cityId,
            String cityName,
            String providerSource
    ) {
        this(name, website, sourceUrl, countryCode, cityId, cityName, providerSource, null, null, null);
    }
}
