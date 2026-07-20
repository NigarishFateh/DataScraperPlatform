package com.datascraper.orchestrator.dto;

import com.datascraper.orchestrator.model.ScrapedData;

import java.util.List;

public record ScrapeResponse(
        String status,
        String message,
        long elapsedMs,
        List<ScrapedData> results
) {
}
