package com.datascraper.orchestrator.scraper.impl;

import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import com.datascraper.orchestrator.scraper.AbstractRemoteScraper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WebsiteRemoteScraper extends AbstractRemoteScraper {

    public WebsiteRemoteScraper(WebClient webClient, IntelligenceScraperProperties properties) {
        super(webClient, properties, "website");
    }

    @Override
    public ScraperType type() {
        return ScraperType.COMPANY_WEBSITE;
    }
}
