package com.datascraper.orchestrator.client;

import com.datascraper.orchestrator.model.DataCategory;
import com.datascraper.orchestrator.model.ScrapedData;
import com.datascraper.orchestrator.model.ScraperSource;

public interface ScraperClient {

    ScraperSource source();

    ScrapedData scrape(DataCategory category);

}
