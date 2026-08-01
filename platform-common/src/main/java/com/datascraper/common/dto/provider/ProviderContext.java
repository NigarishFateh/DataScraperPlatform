package com.datascraper.common.dto.provider;

import java.util.List;

/**
 * Input for a CompanyDataProvider enrichment run.
 */
public record ProviderContext(
        String jobId,
        String companyId,
        String companyName,
        String websiteUrl,
        String countryCode,
        String cityName,
        List<String> categoryIds,
        String correlationId
) {
    public ProviderContext {
        if (categoryIds == null) {
            categoryIds = List.of();
        }
    }
}
