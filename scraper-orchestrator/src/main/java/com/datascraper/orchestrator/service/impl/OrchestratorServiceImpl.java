package com.datascraper.orchestrator.service.impl;

import com.datascraper.orchestrator.client.GoogleScraperClient;
import com.datascraper.orchestrator.client.MicrosoftScraperClient;
import com.datascraper.orchestrator.dto.ScrapeResponse;
import com.datascraper.orchestrator.model.DataCategory;
import com.datascraper.orchestrator.model.ScrapedData;
import com.datascraper.orchestrator.service.OrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class OrchestratorServiceImpl implements OrchestratorService {

    private final GoogleScraperClient googleScraperClient;
    private final MicrosoftScraperClient microsoftScraperClient;
    private final Executor scraperExecutor;

    public OrchestratorServiceImpl(
            GoogleScraperClient googleScraperClient,
            MicrosoftScraperClient microsoftScraperClient,
            @Qualifier("scraperExecutor") Executor scraperExecutor) {
        this.googleScraperClient = googleScraperClient;
        this.microsoftScraperClient = microsoftScraperClient;
        this.scraperExecutor = scraperExecutor;
    }

    @Override
    public ScrapeResponse initiateScrape() {
        log.info("Initiating parallel scrape for category {}", DataCategory.JOBS);
        long startTime = System.currentTimeMillis();

        CompletableFuture<ScrapedData> googleFuture = CompletableFuture.supplyAsync(
                () -> googleScraperClient.scrape(DataCategory.JOBS),
                scraperExecutor
        );

        CompletableFuture<ScrapedData> microsoftFuture = CompletableFuture.supplyAsync(
                () -> microsoftScraperClient.scrape(DataCategory.JOBS),
                scraperExecutor
        );

        CompletableFuture.allOf(googleFuture, microsoftFuture).join();

        long elapsedMs = System.currentTimeMillis() - startTime;
        List<ScrapedData> results = List.of(googleFuture.join(), microsoftFuture.join());

        log.info("Parallel scrape completed in {} ms with {} result sets", elapsedMs, results.size());

        return new ScrapeResponse(
                "SUCCESS",
                "Scraping completed in parallel using the generic ScrapedData model.",
                elapsedMs,
                results
        );
    }

    @Override
    public String getHealthStatus() {
        return "Orchestrator is running and ready to coordinate scraper services.";
    }

}
