package com.datascraper.orchestrator.scraper;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperExecutionStatus;
import com.datascraper.orchestrator.cache.ScraperResultCache;
import com.datascraper.orchestrator.client.ScraperCommunicationException;
import com.datascraper.orchestrator.client.ScraperServiceClient;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Template for remote scraper strategies — cache-aside, retries, and {@link ScraperServiceClient}.
 */
@Slf4j
public abstract class AbstractRemoteScraper implements Scraper {

    private final ScraperServiceClient scraperServiceClient;
    private final ScraperResultCache scraperResultCache;
    private final IntelligenceScraperProperties properties;
    private final String serviceKey;

    protected AbstractRemoteScraper(
            ScraperServiceClient scraperServiceClient,
            ScraperResultCache scraperResultCache,
            IntelligenceScraperProperties properties,
            String serviceKey
    ) {
        this.scraperServiceClient = scraperServiceClient;
        this.scraperResultCache = scraperResultCache;
        this.properties = properties;
        this.serviceKey = serviceKey;
    }

    @Override
    public boolean supports(ScraperContext context) {
        return context.websiteUrl() != null && !context.websiteUrl().isBlank();
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        if (properties.getCache().isEnabled()) {
            Optional<ScraperResult> cached = scraperResultCache.get(type(), context);
            if (cached.isPresent()) {
                log.info("{} scraper cache HIT for company={}", type(), context.companyId());
                return cached.get();
            }
        }

        ScraperResult result = scrapeRemote(context);

        if (properties.getCache().isEnabled() && result.status() == ScraperExecutionStatus.SUCCESS) {
            scraperResultCache.put(type(), context, result);
        }
        return result;
    }

    private ScraperResult scrapeRemote(ScraperContext context) {
        String baseUrl = resolveBaseUrl();
        if (baseUrl == null) {
            return ScraperResult.skipped(type(), "Scraper service URL not configured for " + serviceKey);
        }

        int attempts = 0;
        Exception lastError = null;
        while (attempts < properties.getResilience().getMaxRetries()) {
            attempts++;
            try {
                log.info("Calling {} scraper at {} (attempt {}/{})",
                        type(), baseUrl, attempts, properties.getResilience().getMaxRetries());
                return scraperServiceClient.scrape(baseUrl, type(), context);
            } catch (ScraperCommunicationException ex) {
                lastError = ex;
                log.warn("{} scraper attempt {} failed: {}", type(), attempts, ex.getMessage());
                if (attempts < properties.getResilience().getMaxRetries()) {
                    sleepBeforeRetry();
                }
            }
        }
        return ScraperResult.failed(type(), lastError != null ? lastError.getMessage() : "Unknown scraper error");
    }

    private String resolveBaseUrl() {
        IntelligenceScraperProperties.ServiceEndpoint endpoint = properties.getServices().get(serviceKey);
        return endpoint != null ? endpoint.getBaseUrl() : null;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(properties.getResilience().getRetryDelayMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted for scraper " + type(), ex);
        }
    }
}
