package com.datascraper.news.service;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface NewsScraperService {
    ScraperResult scrape(ScraperContext context);
}
