package com.datascraper.orchestrator.factory;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.scraper.Scraper;

import java.util.List;

/**
 * Factory — selects which scraper strategies to run for a job (Factory Pattern).
 */
public interface ScraperFactory {

    List<Scraper> resolve(ScraperContext context, List<ScraperType> requestedTypes);
}
