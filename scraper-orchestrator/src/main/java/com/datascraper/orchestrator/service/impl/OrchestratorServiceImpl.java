package com.datascraper.orchestrator.service.impl;

import com.datascraper.orchestrator.client.GoogleScraperClient;
import com.datascraper.orchestrator.dto.GoogleScrapeResult;
import com.datascraper.orchestrator.dto.ScrapeResponse;
import com.datascraper.orchestrator.service.OrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorServiceImpl implements OrchestratorService {

    private final GoogleScraperClient googleScraperClient;

    @Override
    public ScrapeResponse initiateScrape() {
        log.info("Initiating scrape via Google scraper microservice");

        GoogleScrapeResult googleResult = googleScraperClient.scrapeJobs();

        return new ScrapeResponse(
                "SUCCESS",
                "Google scrape completed successfully.",
                googleResult
        );
    }

    @Override
    public String getHealthStatus() {
        return "Orchestrator is running and ready to coordinate scraper services.";
    }

}
