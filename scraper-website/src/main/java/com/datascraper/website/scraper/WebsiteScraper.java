package com.datascraper.website.scraper;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

/**
 * Local Strategy inside scraper-website — how we extract data from HTML.
 */
public interface WebsiteScraper {

    ScraperResult scrape(ScraperContext context);
}
