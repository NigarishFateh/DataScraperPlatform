package com.datascraper.orchestrator.dto;

public record ScrapeResponse(
        String status,
        String message,
        GoogleScrapeResult google
) {
}
