/**
 * Service contract for scraping Google pages by category.
 */
package com.datascraper.google.service;

import com.datascraper.google.model.DataCategory;
import com.datascraper.google.model.ScrapedData;

public interface GoogleScraperService {

    ScrapedData scrape(DataCategory category);

}
