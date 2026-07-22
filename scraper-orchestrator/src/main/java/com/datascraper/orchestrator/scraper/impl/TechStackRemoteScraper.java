package com.datascraper.orchestrator.scraper.impl;

import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import com.datascraper.orchestrator.scraper.AbstractRemoteScraper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TechStackRemoteScraper extends AbstractRemoteScraper {

    public TechStackRemoteScraper(WebClient webClient, IntelligenceScraperProperties properties) {
        super(webClient, properties, "tech");
    }

    @Override
    public ScraperType type() {
        return ScraperType.TECHNOLOGY_STACK;
    }
}
