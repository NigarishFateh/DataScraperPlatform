/**
 * Remote scraper strategy that collects GitHub data.
 */
package com.datascraper.orchestrator.scraper.impl;

import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.cache.ScraperResultCache;
import com.datascraper.orchestrator.client.ScraperServiceClient;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import com.datascraper.orchestrator.scraper.AbstractRemoteScraper;
import org.springframework.stereotype.Component;

@Component
public class GitHubRemoteScraper extends AbstractRemoteScraper {

    public GitHubRemoteScraper(
            ScraperServiceClient scraperServiceClient,
            ScraperResultCache scraperResultCache,
            IntelligenceScraperProperties properties
    ) {
        super(scraperServiceClient, scraperResultCache, properties, "github");
    }

    @Override
    public ScraperType type() {
        return ScraperType.GITHUB;
    }
}
