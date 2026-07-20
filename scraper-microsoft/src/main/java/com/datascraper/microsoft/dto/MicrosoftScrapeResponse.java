package com.datascraper.microsoft.dto;

import com.datascraper.microsoft.model.JobListing;

import java.time.Instant;
import java.util.List;

public record MicrosoftScrapeResponse(
        String source,
        Instant scrapedAt,
        String pageTitle,
        int totalJobs,
        List<JobListing> jobs
) {
}
