package com.datascraper.orchestrator.dto;

import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.IntelligenceJobStatus;

import java.util.List;

public record IntelligenceJobResponse(
        String jobId,
        IntelligenceJobStatus status,
        String message,
        long elapsedMs,
        List<ScraperResult> results
) {
}
