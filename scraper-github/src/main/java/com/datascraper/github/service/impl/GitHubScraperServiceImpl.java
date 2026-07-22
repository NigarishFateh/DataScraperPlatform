package com.datascraper.github.service.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.github.factory.GitHubScraperFactory;
import com.datascraper.github.service.GitHubScraperService;
import org.springframework.stereotype.Service;

@Service
public class GitHubScraperServiceImpl implements GitHubScraperService {

    private final GitHubScraperFactory scraperFactory;

    public GitHubScraperServiceImpl(GitHubScraperFactory scraperFactory) {
        this.scraperFactory = scraperFactory;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        return scraperFactory.gitHubOrgScraper().scrape(context);
    }
}
