/**
 * Contract for scraping company social profile links.
 */
package com.datascraper.social.scraper;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface SocialScraper {
    ScraperResult scrape(ScraperContext context);
}
