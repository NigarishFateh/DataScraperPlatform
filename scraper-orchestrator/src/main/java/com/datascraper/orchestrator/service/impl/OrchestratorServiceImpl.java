package com.datascraper.orchestrator.service.impl;

import com.datascraper.orchestrator.client.GoogleScraperClient;
import com.datascraper.orchestrator.client.MicrosoftScraperClient;
import com.datascraper.orchestrator.dto.GoogleScrapeResult;
import com.datascraper.orchestrator.dto.MicrosoftScrapeResult;
import com.datascraper.orchestrator.dto.ScrapeResponse;
import com.datascraper.orchestrator.service.OrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
        log.info("Initiating parallel scrape via Google and Microsoft scraper microservices");
        long startTime = System.currentTimeMillis();

        CompletableFuture<GoogleScrapeResult> googleFuture = CompletableFuture.supplyAsync(
                googleScraperClient::scrapeJobs,
                scraperExecutor
        );

        CompletableFuture<MicrosoftScrapeResult> microsoftFuture = CompletableFuture.supplyAsync(
                microsoftScraperClient::scrapeJobs,
                scraperExecutor
        );

        CompletableFuture.allOf(googleFuture, microsoftFuture).join();

        long elapsedMs = System.currentTimeMillis() - startTime;
        log.info("Parallel scrape completed in {} ms", elapsedMs);

        return new ScrapeResponse(
                "SUCCESS",
                "Google and Microsoft scrapes completed in parallel.",
                googleFuture.join(),
                microsoftFuture.join()
        );
    }

    @Override
    public String getHealthStatus() {
        return "Orchestrator is running and ready to coordinate scraper services.";
    }

}
