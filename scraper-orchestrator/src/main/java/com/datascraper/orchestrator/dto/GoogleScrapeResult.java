package com.datascraper.orchestrator.dto;

import java.time.Instant;
import java.util.List;

public record GoogleScrapeResult(
        String source,
        Instant scrapedAt,
        String pageTitle,
        int totalJobs,
        List<JobListingDto> jobs
) {
}
