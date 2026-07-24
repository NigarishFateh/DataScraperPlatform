/**
 * Contract for scraping company news headlines.
 */
package com.datascraper.news.scraper;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface NewsScraper {
    ScraperResult scrape(ScraperContext context);
}
