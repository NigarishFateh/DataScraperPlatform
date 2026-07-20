package com.datascraper.orchestrator.service.impl;

import com.datascraper.orchestrator.dto.ScrapeResponse;
import com.datascraper.orchestrator.service.OrchestratorService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrchestratorServiceImpl implements OrchestratorService {

    @Override
    public ScrapeResponse initiateScrape() {
        return new ScrapeResponse(
                "PENDING",
                "Scraping is not yet implemented. Scraper services will be connected in upcoming phases.",
                List.of("google", "microsoft", "ibm")
        );
    }

    @Override
    public String getHealthStatus() {
        return "Orchestrator is running and ready to coordinate scraper services.";
    }

}
