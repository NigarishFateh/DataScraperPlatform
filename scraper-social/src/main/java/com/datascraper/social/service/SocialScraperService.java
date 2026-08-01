/**
 * Service contract for running a social scrape.
 */
package com.datascraper.social.service;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface SocialScraperService {
    ScraperResult scrape(ScraperContext context);
}
