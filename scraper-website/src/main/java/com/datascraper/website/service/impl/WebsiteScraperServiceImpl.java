/**
 * Runs a website scrape by calling the website scraper factory.
 */
package com.datascraper.website.service.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.website.factory.WebsiteScraperFactory;
import com.datascraper.website.service.WebsiteScraperService;
import org.springframework.stereotype.Service;

@Service
public class WebsiteScraperServiceImpl implements WebsiteScraperService {

    private final WebsiteScraperFactory scraperFactory;

    public WebsiteScraperServiceImpl(WebsiteScraperFactory scraperFactory) {
        this.scraperFactory = scraperFactory;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        return scraperFactory.companyWebsiteScraper().scrape(context);
    }
}
