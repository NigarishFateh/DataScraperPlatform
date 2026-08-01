/**
 * Runs a social scrape by calling the social scraper factory.
 */
package com.datascraper.social.service.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.social.factory.SocialScraperFactory;
import com.datascraper.social.service.SocialScraperService;
import org.springframework.stereotype.Service;

@Service
public class SocialScraperServiceImpl implements SocialScraperService {

    private final SocialScraperFactory scraperFactory;

    public SocialScraperServiceImpl(SocialScraperFactory scraperFactory) {
        this.scraperFactory = scraperFactory;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        return scraperFactory.companySocialScraper().scrape(context);
    }
}
