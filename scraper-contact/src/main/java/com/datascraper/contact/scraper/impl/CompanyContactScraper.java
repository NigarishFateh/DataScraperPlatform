/**
 * Scrapes contact pages for emails, phones, and related details.
 */
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
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class CompanyContactScraper implements ContactScraper {

    private static final String[] CONTACT_PATHS = {
            "/contact",
            "/contact-us",
            "/contactus",
            "/about/contact",
            "/company/contact",
            "/support/contact",
            "/en/contact",
            "/en/contact-us",
            "/fr/contact",
            "/impressum",
            "/legal/contact"
    };

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
            Document home = pageFetcher.fetch(url);
            String homeUri = home.baseUri().isBlank() ? url : home.baseUri();

            List<Map<String, Object>> items = new ArrayList<>();
            Set<String> seenKeys = new LinkedHashSet<>();
            List<String> pagesVisited = new ArrayList<>();

            mergeItems(items, seenKeys, htmlParser.parse(home, homeUri, properties.getMaxItems()));
            pagesVisited.add(homeUri);

            for (String candidate : discoverContactPages(home, homeUri)) {
                if (items.size() >= properties.getMaxItems()) {
                    break;
                }
                if (pagesVisited.contains(candidate)) {
                    continue;
                }
                try {
                    Document page = pageFetcher.fetch(candidate);
                    String pageUri = page.baseUri().isBlank() ? candidate : page.baseUri();
                    mergeItems(items, seenKeys, htmlParser.parse(page, pageUri, properties.getMaxItems()));
                    pagesVisited.add(pageUri);
                } catch (IOException ex) {
                    log.debug("Contact page fetch skipped {}: {}", candidate, ex.getMessage());
                }
            }

            if (items.size() > properties.getMaxItems()) {
                items = new ArrayList<>(items.subList(0, properties.getMaxItems()));
            }

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourceUrl", homeUri);
            metadata.put("companyId", context.companyId());
            metadata.put("pagesVisited", pagesVisited);

            if (items.isEmpty()) {
                // Empty findings are not a scraper failure — homepage often has no mailto/tel.
                return ScraperResult.success(
                        ScraperType.CONTACT,
                        "No public emails, phones, or addresses found on homepage or contact pages",
                        List.of(),
                        metadata
                );
            }

            return ScraperResult.success(
                    ScraperType.CONTACT,
                    "Extracted %d public contact signal(s) from %d page(s)"
                            .formatted(items.size(), pagesVisited.size()),
                    items,
                    metadata
            );
        } catch (IOException ex) {
            log.warn("Contact scrape failed for {}: {}", url, ex.getMessage());
            return ScraperResult.failed(ScraperType.CONTACT, ex.getMessage());
        }
    }

    private void mergeItems(
            List<Map<String, Object>> target,
            Set<String> seenKeys,
            List<Map<String, Object>> incoming
    ) {
        for (Map<String, Object> item : incoming) {
            String field = String.valueOf(item.getOrDefault("field", ""));
            String value = String.valueOf(item.getOrDefault("value", "")).toLowerCase(Locale.ROOT);
            String key = field + ":" + value;
            if (value.isBlank() || !seenKeys.add(key)) {
                continue;
            }
            target.add(item);
            if (target.size() >= properties.getMaxItems()) {
                return;
            }
        }
    }

    private List<String> discoverContactPages(Document home, String homeUri) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();

        for (Element link : home.select("a[href]")) {
            String abs = link.absUrl("href");
            if (abs.isBlank() || !sameHost(homeUri, abs)) {
                continue;
            }
            String haystack = (abs + " " + link.text()).toLowerCase(Locale.ROOT);
            if (haystack.contains("contact")
                    || haystack.contains("support")
                    || haystack.contains("impressum")
                    || haystack.contains("get-in-touch")
                    || haystack.contains("reach-us")) {
                candidates.add(stripFragment(abs));
            }
        }

        String origin = originOf(homeUri);
        if (origin != null) {
            for (String path : CONTACT_PATHS) {
                candidates.add(origin + path);
            }
        }

        List<String> limited = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(stripFragment(homeUri))) {
                continue;
            }
            limited.add(candidate);
            if (limited.size() >= 6) {
                break;
            }
        }
        return limited;
    }

    private static boolean sameHost(String base, String other) {
        try {
            URI baseUri = URI.create(base);
            URI otherUri = URI.create(other);
            if (baseUri.getHost() == null || otherUri.getHost() == null) {
                return false;
            }
            return baseUri.getHost().equalsIgnoreCase(otherUri.getHost());
        } catch (Exception ex) {
            return false;
        }
    }

    private static String originOf(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            int port = uri.getPort();
            if (port > 0) {
                return uri.getScheme() + "://" + uri.getHost() + ":" + port;
            }
            return uri.getScheme() + "://" + uri.getHost();
        } catch (Exception ex) {
            return null;
        }
    }

    private static String stripFragment(String url) {
        int hash = url.indexOf('#');
        return hash >= 0 ? url.substring(0, hash) : url;
    }
}
