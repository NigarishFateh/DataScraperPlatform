package com.datascraper.tech.service;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface TechScraperService {
    ScraperResult scrape(ScraperContext context);
}
