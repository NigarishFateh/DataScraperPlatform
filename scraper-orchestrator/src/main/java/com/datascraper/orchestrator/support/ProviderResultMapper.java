package com.datascraper.orchestrator.support;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.dto.provider.ProviderContext;
import com.datascraper.common.dto.provider.ProviderResult;
import com.datascraper.common.enums.ProviderExecutionStatus;
import com.datascraper.common.enums.ProviderType;
import com.datascraper.common.enums.ScraperType;

public final class ProviderResultMapper {

    private ProviderResultMapper() {
    }

    public static ScraperContext toScraperContext(ProviderContext context) {
        return new ScraperContext(
                context.jobId(),
                context.companyId(),
                context.companyName(),
                context.websiteUrl(),
                context.categoryIds(),
                context.correlationId()
        );
    }

    public static ProviderResult fromScraperResult(ScraperResult result, ProviderType type, String providerName) {
        ProviderExecutionStatus status = switch (result.status()) {
            case SUCCESS -> ProviderExecutionStatus.SUCCESS;
            case SKIPPED -> ProviderExecutionStatus.SKIPPED;
            case FAILED -> ProviderExecutionStatus.FAILED;
        };
        double confidence = status == ProviderExecutionStatus.SUCCESS ? estimateConfidence(result) : 0.0;
        return new ProviderResult(
                type,
                providerName,
                status,
                result.message(),
                result.scrapedAt(),
                result.items(),
                result.metadata(),
                confidence
        );
    }

    private static double estimateConfidence(ScraperResult result) {
        int itemCount = result.items() != null ? result.items().size() : 0;
        if (itemCount == 0) {
            return 0.35;
        }
        return Math.min(0.95, 0.45 + (itemCount * 0.05));
    }

    public static ProviderType providerTypeFor(ScraperType scraperType) {
        return scraperType.toProviderType();
    }
}
