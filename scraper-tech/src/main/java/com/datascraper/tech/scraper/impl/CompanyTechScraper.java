/**
 * Scrapes a company website for tech stack clues like frameworks and tools.
 */
package com.datascraper.tech.scraper.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.tech.config.TechScraperProperties;
import com.datascraper.tech.scraper.TechScraper;
import com.datascraper.tech.support.HtmlPageFetcher;
import com.datascraper.tech.support.TechStackHtmlParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CompanyTechScraper implements TechScraper {

    private final HtmlPageFetcher pageFetcher;
    private final TechStackHtmlParser htmlParser;
    private final TechScraperProperties properties;

    public CompanyTechScraper(
            HtmlPageFetcher pageFetcher,
            TechStackHtmlParser htmlParser,
            TechScraperProperties properties
    ) {
        this.pageFetcher = pageFetcher;
        this.htmlParser = htmlParser;
        this.properties = properties;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        String url = context.websiteUrl();
        log.info("Tech stack scrape start companyId={} url={}", context.companyId(), url);

        try {
            Document document = pageFetcher.fetch(url);
            List<Map<String, Object>> items = htmlParser.parse(
                    document,
                    document.baseUri(),
                    properties.getMaxItems()
            );

            if (items.isEmpty()) {
                return ScraperResult.failed(
                        ScraperType.TECHNOLOGY_STACK,
                        "No technology signals found at " + document.baseUri()
                );
            }

            return ScraperResult.success(
                    ScraperType.TECHNOLOGY_STACK,
                    "Detected %d technology signal(s)".formatted(items.size()),
                    items,
                    Map.of("sourceUrl", document.baseUri(), "companyId", context.companyId())
            );
        } catch (IOException ex) {
            log.warn("Tech stack scrape failed for {}: {}", url, ex.getMessage());
            return ScraperResult.failed(ScraperType.TECHNOLOGY_STACK, ex.getMessage());
        }
    }
}
