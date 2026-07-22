package com.datascraper.contact.scraper.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.contact.config.ContactScraperProperties;
import com.datascraper.contact.scraper.ContactScraper;
import com.datascraper.contact.support.ContactHtmlParser;
import com.datascraper.contact.support.HtmlPageFetcher;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CompanyContactScraper implements ContactScraper {

    private final HtmlPageFetcher pageFetcher;
    private final ContactHtmlParser htmlParser;
    private final ContactScraperProperties properties;

    public CompanyContactScraper(
            HtmlPageFetcher pageFetcher,
            ContactHtmlParser htmlParser,
            ContactScraperProperties properties
    ) {
        this.pageFetcher = pageFetcher;
        this.htmlParser = htmlParser;
        this.properties = properties;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        String url = context.websiteUrl();
        log.info("Contact scrape start companyId={} url={}", context.companyId(), url);

        try {
            Document document = pageFetcher.fetch(url);
            List<Map<String, Object>> items = htmlParser.parse(
                    document,
                    document.baseUri(),
                    properties.getMaxItems()
            );

            if (items.isEmpty()) {
                return ScraperResult.failed(
                        ScraperType.CONTACT,
                        "No public contact channels found at " + document.baseUri()
                );
            }

            return ScraperResult.success(
                    ScraperType.CONTACT,
                    "Extracted %d public contact signal(s)".formatted(items.size()),
                    items,
                    Map.of("sourceUrl", document.baseUri(), "companyId", context.companyId())
            );
        } catch (IOException ex) {
            log.warn("Contact scrape failed for {}: {}", url, ex.getMessage());
            return ScraperResult.failed(ScraperType.CONTACT, ex.getMessage());
        }
    }
}
