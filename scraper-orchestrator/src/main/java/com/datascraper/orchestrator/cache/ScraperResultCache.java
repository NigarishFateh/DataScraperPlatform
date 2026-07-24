/**
 * Interface for getting and putting cached scraper results.
 */
package com.datascraper.orchestrator.cache;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;

import java.util.Optional;

/**
 * Cache-aside store for {@link ScraperResult} — keyed by company + scraper type + website URL.
 */
public interface ScraperResultCache {

    Optional<ScraperResult> get(ScraperType scraperType, ScraperContext context);

    void put(ScraperType scraperType, ScraperContext context, ScraperResult result);
}
