/**
 * Runs a news scrape by calling the news scraper factory.
 */
package com.datascraper.news.service.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.news.factory.NewsScraperFactory;
import com.datascraper.news.service.NewsScraperService;
import org.springframework.stereotype.Service;

@Service
public class NewsScraperServiceImpl implements NewsScraperService {

    private final NewsScraperFactory scraperFactory;

    public NewsScraperServiceImpl(NewsScraperFactory scraperFactory) {
        this.scraperFactory = scraperFactory;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        return scraperFactory.companyNewsScraper().scrape(context);
    }
}
