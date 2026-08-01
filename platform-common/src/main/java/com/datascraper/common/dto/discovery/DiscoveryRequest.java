package com.datascraper.common.dto.discovery;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Criteria for company discovery. Categories are required; geography is optional.
 */
public record DiscoveryRequest(
        String jobId,
        String correlationId,
        List<String> countryCodes,
        List<String> cityIds,
        @NotEmpty List<String> categoryIds,
        int maxResults
) {
    public DiscoveryRequest {
        if (countryCodes == null) {
            countryCodes = List.of();
        }
        if (cityIds == null) {
            cityIds = List.of();
        }
        if (maxResults <= 0) {
            maxResults = 500;
        }
    }
}
