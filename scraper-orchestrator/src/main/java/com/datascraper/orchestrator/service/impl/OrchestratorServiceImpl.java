package com.datascraper.orchestrator.service.impl;

import com.datascraper.orchestrator.client.ScraperClient;
import com.datascraper.orchestrator.client.ScraperClientRegistry;
import com.datascraper.orchestrator.dto.ScrapeRequest;
import com.datascraper.orchestrator.dto.ScrapeResponse;
import com.datascraper.orchestrator.model.DataCategory;
import com.datascraper.orchestrator.model.ScrapedData;
import com.datascraper.orchestrator.model.ScraperSource;
import com.datascraper.orchestrator.service.OrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class OrchestratorServiceImpl implements OrchestratorService {

    private final ScraperClientRegistry scraperClientRegistry;
    private final Executor scraperExecutor;

    public OrchestratorServiceImpl(
            ScraperClientRegistry scraperClientRegistry,
            @Qualifier("scraperExecutor") Executor scraperExecutor) {
        this.scraperClientRegistry = scraperClientRegistry;
        this.scraperExecutor = scraperExecutor;
    }

    @Override
    public ScrapeResponse initiateScrape(ScrapeRequest request) {
        log.info("Initiating parallel scrape for sources={} categories={}", request.sources(), request.categories());
        long startTime = System.currentTimeMillis();

        List<CompletableFuture<ScrapedData>> futures = new ArrayList<>();

        for (ScraperSource source : request.sources()) {
            ScraperClient client = scraperClientRegistry.getClient(source);

            for (DataCategory category : request.categories()) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> client.scrape(category),
                        scraperExecutor
                ));
            }
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        List<ScrapedData> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        long elapsedMs = System.currentTimeMillis() - startTime;
        long failedCount = results.stream()
                .filter(result -> "FAILED".equals(result.metadata().getOrDefault("status", "SUCCESS")))
                .count();

        String status = failedCount == 0 ? "SUCCESS" : failedCount == results.size() ? "FAILED" : "PARTIAL_SUCCESS";
        String message = "Completed %d scrape tasks in parallel (%d failed).".formatted(results.size(), failedCount);

        log.info("Parallel scrape finished with status={} elapsedMs={}", status, elapsedMs);

        return new ScrapeResponse(status, message, elapsedMs, results);
    }

    @Override
    public String getHealthStatus() {
        return "Orchestrator is running and ready to coordinate scraper services.";
    }

}
