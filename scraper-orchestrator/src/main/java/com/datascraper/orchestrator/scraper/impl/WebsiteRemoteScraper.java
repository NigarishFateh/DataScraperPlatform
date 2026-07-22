package com.datascraper.orchestrator.scraper.impl;

import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.client.ScraperServiceClient;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import com.datascraper.orchestrator.scraper.AbstractRemoteScraper;
import org.springframework.stereotype.Component;
import com.datascraper.orchestrator.client.ScraperServiceClient;

@Component
public class WebsiteRemoteScraper extends AbstractRemoteScraper {

    public WebsiteRemoteScraper(ScraperServiceClient scraperServiceClient, IntelligenceScraperProperties properties) {
        super(scraperServiceClient, properties, "website");
    }

    @Override
    public ScraperType type() {
        return ScraperType.COMPANY_WEBSITE;
    }
}
