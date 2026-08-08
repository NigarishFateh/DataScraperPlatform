package com.datascraper.discovery.dto;

import java.util.List;

/**
 * Human-readable discovery criteria resolved from catalog IDs.
 */
public record ResolvedDiscoveryCriteria(
        List<String> categoryIds,
        List<String> categoryNames,
        List<String> countryCodes,
        List<String> countryNames,
        List<String> cityIds,
        List<String> cityNames,
        List<String> searchKeywords,
        int maxResults,
        List<String> companyNames
) {
    public ResolvedDiscoveryCriteria {
        if (companyNames == null) {
            companyNames = List.of();
        }
    }

    public boolean hasCompanyNames() {
        return companyNames != null && !companyNames.isEmpty();
    }
}
