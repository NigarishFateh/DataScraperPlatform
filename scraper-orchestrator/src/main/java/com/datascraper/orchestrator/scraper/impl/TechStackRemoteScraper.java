/**
 * Remote scraper strategy that collects technology stack data.
 */
package com.datascraper.orchestrator.scraper.impl;

import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.cache.ScraperResultCache;
import com.datascraper.orchestrator.client.ScraperServiceClient;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import com.datascraper.orchestrator.scraper.AbstractRemoteScraper;
import org.springframework.stereotype.Component;

@Component
public class TechStackRemoteScraper extends AbstractRemoteScraper {

    public TechStackRemoteScraper(
            ScraperServiceClient scraperServiceClient,
            ScraperResultCache scraperResultCache,
            IntelligenceScraperProperties properties
    ) {
        super(scraperServiceClient, scraperResultCache, properties, "tech");
    }

    @Override
    public ScraperType type() {
        return ScraperType.TECHNOLOGY_STACK;
    }
}
