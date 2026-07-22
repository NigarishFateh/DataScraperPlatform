package com.datascraper.website.service;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface WebsiteScraperService {

    ScraperResult scrape(ScraperContext context);
}
