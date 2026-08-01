package com.datascraper.common.dto.provider;

import com.datascraper.common.enums.ProviderExecutionStatus;
import com.datascraper.common.enums.ProviderType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Normalized enrichment output from any CompanyDataProvider.
 */
public record ProviderResult(
        ProviderType providerType,
        String providerName,
        ProviderExecutionStatus status,
        String message,
        Instant scrapedAt,
        List<Map<String, Object>> items,
        Map<String, Object> metadata,
        double confidence
) {
    public static ProviderResult success(
            ProviderType type,
            String providerName,
            String message,
            List<Map<String, Object>> items,
            Map<String, Object> metadata,
            double confidence
    ) {
        return new ProviderResult(
                type,
                providerName,
                ProviderExecutionStatus.SUCCESS,
                message,
                Instant.now(),
                items == null ? List.of() : items,
                metadata == null ? Map.of() : metadata,
                confidence
        );
    }

    public static ProviderResult failed(ProviderType type, String providerName, String message) {
        return new ProviderResult(
                type,
                providerName,
                ProviderExecutionStatus.FAILED,
                message,
                Instant.now(),
                List.of(),
                Map.of("error", message),
                0.0
        );
    }

    public static ProviderResult skipped(ProviderType type, String providerName, String message) {
        return new ProviderResult(
                type,
                providerName,
                ProviderExecutionStatus.SKIPPED,
                message,
                Instant.now(),
                List.of(),
                Map.of("reason", message),
                0.0
        );
    }
}
