/**
 * Service contract for running a website scrape.
 */
package com.datascraper.website.service;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface WebsiteScraperService {

    ScraperResult scrape(ScraperContext context);
}
