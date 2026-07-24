/**
 * Shared HTTP client base that calls a scraper service with retries and timeouts.
 */
package com.datascraper.orchestrator.client;

import com.datascraper.orchestrator.config.ScraperResilienceProperties;
import com.datascraper.orchestrator.model.DataCategory;
import com.datascraper.orchestrator.model.ScrapedData;
import com.datascraper.orchestrator.model.ScraperSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractScraperClient implements ScraperClient {

    private final WebClient webClient;
    private final String baseUrl;
    private final ScraperSource source;
    private final ScraperResilienceProperties resilienceProperties;

    protected AbstractScraperClient(
            WebClient webClient,
            String baseUrl,
            ScraperSource source,
            ScraperResilienceProperties resilienceProperties) {
        this.webClient = webClient;
        this.baseUrl = baseUrl;
        this.source = source;
        this.resilienceProperties = resilienceProperties;
    }

    @Override
    public ScraperSource source() {
        return source;
    }

    @Override
    public ScrapedData scrape(DataCategory category) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < resilienceProperties.maxRetries()) {
            attempts++;
            try {
                return executeScrape(category);
            } catch (Exception exception) {
                lastException = exception;
                log.warn(
                        "Scrape attempt {}/{} failed for source={} category={}: {}",
                        attempts,
                        resilienceProperties.maxRetries(),
                        source,
                        category,
                        exception.getMessage()
                );

                if (attempts < resilienceProperties.maxRetries()) {
                    sleepBeforeRetry();
                }
            }
        }

        return buildFallbackResult(category, lastException);
    }

    private ScrapedData executeScrape(DataCategory category) {
        String url = baseUrl + "/api/scrape/" + category.name().toLowerCase();
        log.info("Calling {} scraper at {} (timeout={}ms)", source, url, resilienceProperties.timeoutMs());

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(ScrapedData.class)
                .timeout(Duration.ofMillis(resilienceProperties.timeoutMs()))
                .block();
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(resilienceProperties.retryDelayMs());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted for source " + source, interruptedException);
        }
    }

    private ScrapedData buildFallbackResult(DataCategory category, Exception exception) {
        String errorMessage = exception != null ? exception.getMessage() : "Unknown error";

        if (exception instanceof WebClientResponseException webClientException) {
            errorMessage = "HTTP " + webClientException.getStatusCode().value()
                    + " - " + webClientException.getStatusText();
        }

        log.error("All scrape attempts failed for source={} category={}. Returning fallback.", source, category);

        return new ScrapedData(
                source.name().toLowerCase(),
                category,
                Instant.now(),
                "Scrape failed after retries",
                0,
                List.of(),
                Map.of(
                        "status", "FAILED",
                        "error", errorMessage,
                        "attempts", String.valueOf(resilienceProperties.maxRetries())
                )
        );
    }

}
