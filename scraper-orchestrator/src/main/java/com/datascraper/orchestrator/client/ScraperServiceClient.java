package com.datascraper.orchestrator.client;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;

/**
 * Facade for orchestrator → scraper microservice HTTP calls (WebClient + JSON DTOs).
 */
public interface ScraperServiceClient {

    ScraperResult scrape(String baseUrl, ScraperType scraperType, ScraperContext context);
}
