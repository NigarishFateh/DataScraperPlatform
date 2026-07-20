package com.datascraper.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scraper.resilience")
public record ScraperResilienceProperties(
        int timeoutMs,
        int maxRetries,
        long retryDelayMs
) {
}
