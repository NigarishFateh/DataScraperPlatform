package com.datascraper.contact.scraper;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface ContactScraper {
    ScraperResult scrape(ScraperContext context);
}
