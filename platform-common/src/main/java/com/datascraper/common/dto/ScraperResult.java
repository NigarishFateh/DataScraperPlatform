/**
 * Carries response data for the result of a scraper run.
 */
package com.datascraper.common.dto;

import com.datascraper.common.enums.ScraperExecutionStatus;
import com.datascraper.common.enums.ScraperType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Normalized output from any scraper strategy — orchestrator merges these into a report.
 */
public record ScraperResult(
        ScraperType scraperType,
        ScraperExecutionStatus status,
        String message,
        Instant scrapedAt,
        List<Map<String, Object>> items,
        Map<String, Object> metadata
) {
    public static ScraperResult success(
            ScraperType type,
            String message,
            List<Map<String, Object>> items,
            Map<String, Object> metadata
    ) {
        return new ScraperResult(
                type,
                ScraperExecutionStatus.SUCCESS,
                message,
                Instant.now(),
                items,
                metadata
        );
    }

    public static ScraperResult failed(ScraperType type, String message) {
        return new ScraperResult(
                type,
                ScraperExecutionStatus.FAILED,
                message,
                Instant.now(),
                List.of(),
                Map.of("error", message)
        );
    }

    public static ScraperResult skipped(ScraperType type, String message) {
        return new ScraperResult(
                type,
                ScraperExecutionStatus.SKIPPED,
                message,
                Instant.now(),
                List.of(),
                Map.of("reason", message)
        );
    }
}
