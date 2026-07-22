package com.datascraper.orchestrator.scraper;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Template for remote scraper strategies — handles HTTP, timeout, retries.
 * Concrete subclasses only declare {@link #type()} and service config key.
 */
@Slf4j
public abstract class AbstractRemoteScraper implements Scraper {

    private final WebClient webClient;
    private final IntelligenceScraperProperties properties;
    private final String serviceKey;

    protected AbstractRemoteScraper(
            WebClient webClient,
            IntelligenceScraperProperties properties,
            String serviceKey
    ) {
        this.webClient = webClient;
        this.properties = properties;
        this.serviceKey = serviceKey;
    }

    @Override
    public boolean supports(ScraperContext context) {
        return context.websiteUrl() != null && !context.websiteUrl().isBlank();
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        String baseUrl = resolveBaseUrl();
        if (baseUrl == null) {
            return ScraperResult.skipped(type(), "Scraper service URL not configured for " + serviceKey);
        }

        int attempts = 0;
        Exception lastError = null;
        while (attempts < properties.getResilience().getMaxRetries()) {
            attempts++;
            try {
                log.info("Calling {} scraper at {}/api/scrape (attempt {}/{})",
                        type(), baseUrl, attempts, properties.getResilience().getMaxRetries());
                ScraperResult result = webClient.post()
                        .uri(baseUrl + "/api/scrape")
                        .bodyValue(context)
                        .retrieve()
                        .bodyToMono(ScraperResult.class)
                        .timeout(Duration.ofMillis(properties.getResilience().getTimeoutMs()))
                        .block();
                return result != null ? result : ScraperResult.failed(type(), "Empty response from scraper service");
            } catch (Exception ex) {
                lastError = ex;
                log.warn("{} scraper attempt {} failed: {}", type(), attempts, ex.getMessage());
                if (attempts < properties.getResilience().getMaxRetries()) {
                    sleepBeforeRetry();
                }
            }
        }
        return ScraperResult.failed(type(), formatError(lastError));
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

    private String formatError(Exception exception) {
        if (exception instanceof WebClientResponseException webEx) {
            return "HTTP " + webEx.getStatusCode().value() + " from " + type() + " scraper";
        }
        return exception != null ? exception.getMessage() : "Unknown scraper error";
    }
}
