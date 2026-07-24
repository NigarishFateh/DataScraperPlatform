/**
 * Contract for scraping GitHub organization data.
 */
package com.datascraper.github.scraper;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface GitHubScraper {
    ScraperResult scrape(ScraperContext context);
}
