package com.datascraper.common.dto.job;

import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request to create an asynchronous scraping job.
 */
public record CreateJobRequest(
        @NotEmpty List<String> categoryIds,
        List<String> countryCodes,
        List<String> cityIds,
        List<String> enabledProviders,
        Integer maxCompanies,
        Map<String, Object> options
) {
    public CreateJobRequest {
        if (countryCodes == null) {
            countryCodes = List.of();
        }
        if (cityIds == null) {
            cityIds = List.of();
        }
        if (enabledProviders == null) {
            enabledProviders = List.of();
        }
        if (maxCompanies == null) {
            maxCompanies = 500;
        } else if (maxCompanies < 0) {
            // UI "Unlimited" sentinel (-1) → practical soft ceiling for API/enrichment safety.
            maxCompanies = 100_000;
        } else if (maxCompanies > 100_000) {
            maxCompanies = 100_000;
        }
        if (options == null) {
            options = Map.of();
        }
    }
}
