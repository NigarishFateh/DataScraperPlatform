package com.datascraper.orchestrator.service;

import com.datascraper.orchestrator.dto.ScrapeResponse;

public interface OrchestratorService {

    ScrapeResponse initiateScrape();

    String getHealthStatus();

}
