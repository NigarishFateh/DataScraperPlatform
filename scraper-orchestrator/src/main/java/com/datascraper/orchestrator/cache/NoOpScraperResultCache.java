/**
 * Cache that does nothing — always misses and never stores results.
 */
package com.datascraper.orchestrator.cache;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;

import java.util.Optional;

public class NoOpScraperResultCache implements ScraperResultCache {

    @Override
    public Optional<ScraperResult> get(ScraperType scraperType, ScraperContext context) {
        return Optional.empty();
    }

    @Override
    public void put(ScraperType scraperType, ScraperContext context, ScraperResult result) {
        // no-op
    }
}
