package com.datascraper.common.dto;

import java.util.List;

/**
 * Input passed to every {@code Scraper} strategy — company-agnostic, source-oriented.
 */
public record ScraperContext(
        String jobId,
        String companyId,
        String companyName,
        String websiteUrl,
        List<String> categoryIds,
        String correlationId
) {
}
