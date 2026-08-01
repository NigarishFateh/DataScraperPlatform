/**
 * Scrapes a company website for public social profile links.
 */
package com.datascraper.social.scraper.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.social.config.SocialScraperProperties;
import com.datascraper.social.scraper.SocialScraper;
import com.datascraper.social.support.HtmlPageFetcher;
import com.datascraper.social.support.RobotsTxtGuard;
import com.datascraper.social.support.SocialHtmlParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CompanySocialScraper implements SocialScraper {

    private final HtmlPageFetcher pageFetcher;
    private final SocialHtmlParser htmlParser;
    private final RobotsTxtGuard robotsTxtGuard;
    private final SocialScraperProperties properties;

    public CompanySocialScraper(
            HtmlPageFetcher pageFetcher,
            SocialHtmlParser htmlParser,
            RobotsTxtGuard robotsTxtGuard,
            SocialScraperProperties properties
    ) {
        this.pageFetcher = pageFetcher;
        this.htmlParser = htmlParser;
        this.robotsTxtGuard = robotsTxtGuard;
        this.properties = properties;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        String url = context.websiteUrl();
        if (url == null || url.isBlank()) {
            return ScraperResult.skipped(ScraperType.SOCIAL, "No website URL provided");
        }

        log.info("Social scrape start companyId={} url={}", context.companyId(), url);

        try {
            robotsTxtGuard.verifyAllowed(url);
            Document document = pageFetcher.fetch(url);
            String sourceUrl = document.baseUri().isBlank() ? url : document.baseUri();
            List<Map<String, Object>> items = htmlParser.parse(
                    document,
                    sourceUrl,
                    properties.getMaxItems()
            );

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourceUrl", sourceUrl);
            metadata.put("companyId", context.companyId());

            if (items.isEmpty()) {
                return ScraperResult.success(
                        ScraperType.SOCIAL,
                        "No social profile links found on homepage",
                        List.of(),
                        metadata
                );
            }

            return ScraperResult.success(
                    ScraperType.SOCIAL,
                    "Extracted %d social profile link(s)".formatted(items.size()),
                    items,
                    metadata
            );
        } catch (IOException ex) {
            log.warn("Social scrape failed for {}: {}", url, ex.getMessage());
            return ScraperResult.failed(ScraperType.SOCIAL, ex.getMessage());
        }
    }
}
