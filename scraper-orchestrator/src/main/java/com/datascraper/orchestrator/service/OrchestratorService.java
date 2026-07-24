/**
 * Service interface for multi-source scrapes and health status.
 */
package com.datascraper.orchestrator.service;

import com.datascraper.orchestrator.dto.ScrapeRequest;
import com.datascraper.orchestrator.dto.ScrapeResponse;

public interface OrchestratorService {

    ScrapeResponse initiateScrape(ScrapeRequest request);

    String getHealthStatus();

}
