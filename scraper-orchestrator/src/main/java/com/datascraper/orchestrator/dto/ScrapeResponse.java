package com.datascraper.orchestrator.dto;

import java.util.List;

public record ScrapeResponse(
        String status,
        String message,
        List<String> sources
) {
}
