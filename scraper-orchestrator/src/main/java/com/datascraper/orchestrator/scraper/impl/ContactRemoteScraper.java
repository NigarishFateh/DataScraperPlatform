package com.datascraper.orchestrator.scraper.impl;

import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.client.ScraperServiceClient;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import com.datascraper.orchestrator.scraper.AbstractRemoteScraper;
import org.springframework.stereotype.Component;

@Component
public class ContactRemoteScraper extends AbstractRemoteScraper {

    public ContactRemoteScraper(ScraperServiceClient scraperServiceClient, IntelligenceScraperProperties properties) {
        super(scraperServiceClient, properties, "contact");
    }

    @Override
    public ScraperType type() {
        return ScraperType.CONTACT;
    }
}
