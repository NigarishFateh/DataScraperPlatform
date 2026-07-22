package com.datascraper.github.factory;

import com.datascraper.github.scraper.GitHubScraper;
import com.datascraper.github.scraper.impl.GitHubOrgScraper;
import org.springframework.stereotype.Component;

@Component
public class GitHubScraperFactory {

    private final GitHubOrgScraper gitHubOrgScraper;

    public GitHubScraperFactory(GitHubOrgScraper gitHubOrgScraper) {
        this.gitHubOrgScraper = gitHubOrgScraper;
    }

    public GitHubScraper gitHubOrgScraper() {
        return gitHubOrgScraper;
    }
}
