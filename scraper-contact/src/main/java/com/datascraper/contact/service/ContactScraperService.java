/**
 * Service contract for running a contact scrape.
 */
package com.datascraper.contact.service;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;

public interface ContactScraperService {
    ScraperResult scrape(ScraperContext context);
}
