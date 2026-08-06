/**
 * Scrapes a company website for public info while checking robots.txt.
 */
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
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class CompanyWebsiteScraper implements WebsiteScraper {

    private static final int MAX_EXTRA_PAGES = 3;

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
            List<Map<String, Object>> items = new ArrayList<>(
                    htmlParser.parse(document, document.baseUri(), properties.getMaxItems())
            );

            for (String extraUrl : findSupplementaryPages(document, document.baseUri())) {
                try {
                    robotsTxtGuard.verifyAllowed(extraUrl);
                    Document extra = pageFetcher.fetch(extraUrl);
                    items.addAll(htmlParser.parse(extra, extra.baseUri(), Math.min(40, properties.getMaxItems())));
                } catch (Exception ex) {
                    log.debug("Extra page scrape skipped for {}: {}", extraUrl, ex.getMessage());
                }
            }

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

    private List<String> findSupplementaryPages(Document document, String baseUri) {
        Set<String> urls = new LinkedHashSet<>();
        for (Element link : document.select(
                "a[href*='contact'], a[href*='about'], a[href*='team'], a[href*='over-ons'], a[href*='overons'], "
                        + "a[href*='management'], a[href*='leadership'], a[href*='ons-team'], a[href*='founders'], "
                        + "a[href*='impressum']")) {
            String href = link.absUrl("href");
            if (href.isBlank() || !sameHost(baseUri, href)) {
                continue;
            }
            String lower = href.toLowerCase(Locale.ROOT);
            if (lower.contains("career") || lower.contains("job") || lower.contains("blog") || lower.contains("news")) {
                continue;
            }
            urls.add(href.split("#")[0]);
            if (urls.size() >= MAX_EXTRA_PAGES) {
                break;
            }
        }
        return List.copyOf(urls);
    }

    private static boolean sameHost(String baseUri, String href) {
        try {
            String baseHost = URI.create(baseUri).getHost();
            String hrefHost = URI.create(href).getHost();
            if (baseHost == null || hrefHost == null) {
                return false;
            }
            return baseHost.equalsIgnoreCase(hrefHost);
        } catch (Exception ex) {
            return false;
        }
    }
}
