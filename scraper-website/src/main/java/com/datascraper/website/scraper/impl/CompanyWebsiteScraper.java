package com.datascraper.website.scraper.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.website.config.WebsiteScraperProperties;
import com.datascraper.website.scraper.WebsiteScraper;
import com.datascraper.website.support.CompanyWebsiteHtmlParser;
import com.datascraper.website.support.HtmlPageFetcher;
import com.datascraper.website.support.RobotsTxtGuard;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CompanyWebsiteScraper implements WebsiteScraper {

    private final HtmlPageFetcher pageFetcher;
    private final CompanyWebsiteHtmlParser htmlParser;
    private final RobotsTxtGuard robotsTxtGuard;
    private final WebsiteScraperProperties properties;

    public CompanyWebsiteScraper(
            HtmlPageFetcher pageFetcher,
            CompanyWebsiteHtmlParser htmlParser,
            RobotsTxtGuard robotsTxtGuard,
            WebsiteScraperProperties properties
    ) {
        this.pageFetcher = pageFetcher;
        this.htmlParser = htmlParser;
        this.robotsTxtGuard = robotsTxtGuard;
        this.properties = properties;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        String url = context.websiteUrl();
        log.info("Website scrape start companyId={} url={}", context.companyId(), url);

        try {
            robotsTxtGuard.verifyAllowed(url);
            Document document = pageFetcher.fetch(url);
            List<Map<String, Object>> items = htmlParser.parse(document, document.baseUri(), properties.getMaxItems());

            if (items.isEmpty()) {
                return ScraperResult.failed(
                        ScraperType.COMPANY_WEBSITE,
                        "No public website signals found at " + document.baseUri()
                );
            }

            return ScraperResult.success(
                    ScraperType.COMPANY_WEBSITE,
                    "Extracted %d public website field(s)".formatted(items.size()),
                    items,
                    Map.of(
                            "sourceUrl", document.baseUri(),
                            "pageTitle", document.title(),
                            "companyId", context.companyId()
                    )
            );
        } catch (IOException ex) {
            log.warn("Website scrape failed for {}: {}", url, ex.getMessage());
            return ScraperResult.failed(ScraperType.COMPANY_WEBSITE, ex.getMessage());
        }
    }
}
