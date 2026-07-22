package com.datascraper.tech.scraper;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface TechScraper {
    ScraperResult scrape(ScraperContext context);
}
