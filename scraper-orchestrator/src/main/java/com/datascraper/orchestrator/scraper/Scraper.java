package com.datascraper.orchestrator.scraper;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;

/**
 * Strategy interface — one implementation per data-source capability (Open/Closed Principle).
 * <p>
 * Adding LinkedIn later = new class implementing {@code Scraper}, register in Spring context.
 * Orchestrator code does not change.
 */
public interface Scraper {

    ScraperType type();

    /** Whether this strategy should run for the given job context. */
    boolean supports(ScraperContext context);

    /** Execute scrape and return a normalized partial result. */
    ScraperResult scrape(ScraperContext context);
}
