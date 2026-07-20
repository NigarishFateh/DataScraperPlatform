package com.datascraper.ibm.service;

import com.datascraper.ibm.model.DataCategory;
import com.datascraper.ibm.model.ScrapedData;

public interface IbmScraperService {

    ScrapedData scrape(DataCategory category);

}
