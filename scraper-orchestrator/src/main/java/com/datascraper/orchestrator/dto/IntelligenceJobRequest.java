/**
 * Request body for starting an intelligence scrape job.
 */
package com.datascraper.orchestrator.dto;

import com.datascraper.common.enums.ScraperType;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record IntelligenceJobRequest(
        @NotBlank String companyId,
        @NotBlank String companyName,
        @NotBlank String websiteUrl,
        List<String> categoryIds,
        List<ScraperType> scraperTypes
) {
}
