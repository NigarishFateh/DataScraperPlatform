/**
 * Scrapes recent news headlines for a company from an RSS feed.
 */
package com.datascraper.news.scraper.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.news.scraper.NewsScraper;
import com.datascraper.news.support.NewsRssFetcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CompanyNewsScraper implements NewsScraper {

    private final NewsRssFetcher newsRssFetcher;

    public CompanyNewsScraper(NewsRssFetcher newsRssFetcher) {
        this.newsRssFetcher = newsRssFetcher;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        String companyName = context.companyName();
        log.info("News scrape start companyId={} name={}", context.companyId(), companyName);

        try {
            List<Map<String, Object>> items = newsRssFetcher.fetchHeadlines(companyName);
            if (items.isEmpty()) {
                return ScraperResult.failed(
                        ScraperType.NEWS,
                        "No recent news headlines found for " + companyName
                );
            }

            return ScraperResult.success(
                    ScraperType.NEWS,
                    "Found %d news headline(s)".formatted(items.size()),
                    items,
                    Map.of("companyName", companyName, "companyId", context.companyId())
            );
        } catch (IOException ex) {
            log.warn("News scrape failed for {}: {}", companyName, ex.getMessage());
            return ScraperResult.failed(ScraperType.NEWS, ex.getMessage());
        }
    }
}
