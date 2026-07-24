/**
 * Service contract for running a GitHub scrape.
 */
package com.datascraper.github.service;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface GitHubScraperService {
    ScraperResult scrape(ScraperContext context);
}
