package com.datascraper.microsoft.service;

import com.datascraper.microsoft.model.DataCategory;
import com.datascraper.microsoft.model.ScrapedData;

public interface MicrosoftScraperService {

    ScrapedData scrape(DataCategory category);

}
