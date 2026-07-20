package com.datascraper.google.dto;

import com.datascraper.google.model.JobListing;

import java.time.Instant;
import java.util.List;

public record GoogleScrapeResponse(
        String source,
        Instant scrapedAt,
        String pageTitle,
        int totalJobs,
        List<JobListing> jobs
) {
}
