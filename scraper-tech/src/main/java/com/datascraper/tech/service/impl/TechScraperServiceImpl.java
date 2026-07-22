package com.datascraper.tech.service.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.tech.factory.TechScraperFactory;
import com.datascraper.tech.service.TechScraperService;
import org.springframework.stereotype.Service;

@Service
public class TechScraperServiceImpl implements TechScraperService {

    private final TechScraperFactory scraperFactory;

    public TechScraperServiceImpl(TechScraperFactory scraperFactory) {
        this.scraperFactory = scraperFactory;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        return scraperFactory.companyTechScraper().scrape(context);
    }
}
