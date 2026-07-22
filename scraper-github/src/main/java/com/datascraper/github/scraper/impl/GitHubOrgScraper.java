package com.datascraper.github.scraper.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.github.client.GitHubSearchClient;
import com.datascraper.github.scraper.GitHubScraper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GitHubOrgScraper implements GitHubScraper {

    private final GitHubSearchClient gitHubSearchClient;

    public GitHubOrgScraper(GitHubSearchClient gitHubSearchClient) {
        this.gitHubSearchClient = gitHubSearchClient;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        String companyName = context.companyName();
        log.info("GitHub scrape start companyId={} name={}", context.companyId(), companyName);

        try {
            List<Map<String, Object>> items = gitHubSearchClient.searchOrganizations(companyName);
            if (items.isEmpty()) {
                return ScraperResult.failed(
                        ScraperType.GITHUB,
                        "No public GitHub organizations matched " + companyName
                );
            }

            return ScraperResult.success(
                    ScraperType.GITHUB,
                    "Found %d GitHub organization match(es)".formatted(items.size()),
                    items,
                    Map.of("companyName", companyName, "companyId", context.companyId())
            );
        } catch (RuntimeException ex) {
            log.warn("GitHub scrape failed for {}: {}", companyName, ex.getMessage());
            return ScraperResult.failed(ScraperType.GITHUB, ex.getMessage());
        }
    }
}
