/**
 * Parses IBM HTML pages into scraped items by category.
 */
package com.datascraper.ibm.support;

import com.datascraper.ibm.model.DataCategory;
import com.datascraper.ibm.model.ScrapedItem;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HtmlScrapeParser {

    private HtmlScrapeParser() {
    }

    public static List<ScrapedItem> parse(Document document, DataCategory category, String fallbackUrl) {
        return switch (category) {
            case JOBS -> parseJobs(document, fallbackUrl);
            case PRODUCTS -> parseProducts(document, fallbackUrl);
            case SERVICES -> parseServices(document, fallbackUrl);
            case COMPANY_INFO -> parseCompanyInfo(document, fallbackUrl);
            case CONTACTS -> parseContacts(document, fallbackUrl);
            case NEWS -> parseNews(document, fallbackUrl);
        };
    }

    private static List<ScrapedItem> parseJobs(Document document, String fallbackUrl) {
        List<ScrapedItem> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        addFromLinks(document.select(
                "a[href*='job'], a[href*='career'], a[href*='jobs/'], a[href*='jobdetails']"),
                items, seen, fallbackUrl, Map.of("type", "job"));

        addFromHeadings(document.select("h2, h3, h4"), items, seen, fallbackUrl, Map.of("type", "job"));

        return items;
    }

    private static List<ScrapedItem> parseProducts(Document document, String fallbackUrl) {
        List<ScrapedItem> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        addFromLinks(document.select(
                "a[href*='product'], a[href*='products/'], a[href*='software'], a[href*='storage']"),
                items, seen, fallbackUrl, Map.of("type", "product"));

        addFromHeadings(document.select("h2, h3, [class*='product'], [class*='card'] h3"),
                items, seen, fallbackUrl, Map.of("type", "product"));

        return items;
    }

    private static List<ScrapedItem> parseServices(Document document, String fallbackUrl) {
        List<ScrapedItem> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        addFromLinks(document.select(
                "a[href*='service'], a[href*='solution'], a[href*='consulting'], a[href*='cloud']"),
                items, seen, fallbackUrl, Map.of("type", "service"));

        addFromHeadings(document.select("h2, h3, [class*='service'], [class*='solution']"),
                items, seen, fallbackUrl, Map.of("type", "service"));

        return items;
    }

    private static List<ScrapedItem> parseCompanyInfo(Document document, String fallbackUrl) {
        List<ScrapedItem> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        String metaDescription = metaContent(document, "description");
        if (metaDescription != null && seen.add(metaDescription)) {
            items.add(new ScrapedItem(
                    document.title(),
                    metaDescription,
                    fallbackUrl,
                    null,
                    null,
                    Map.of("type", "company-info", "field", "meta-description")
            ));
        }

        addFromHeadings(document.select("h1, h2"), items, seen, fallbackUrl, Map.of("type", "company-info"));

        for (Element paragraph : document.select("p")) {
            String text = paragraph.text().trim();
            if (text.length() < 60 || !seen.add(text)) {
                continue;
            }
            items.add(new ScrapedItem(
                    truncate(text, 80),
                    text,
                    fallbackUrl,
                    null,
                    null,
                    Map.of("type", "company-info", "field", "paragraph")
            ));
            if (items.size() >= 15) {
                break;
            }
        }

        return items;
    }

    private static List<ScrapedItem> parseContacts(Document document, String fallbackUrl) {
        List<ScrapedItem> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (Element link : document.select("a[href^='mailto:'], a[href^='tel:']")) {
            String href = link.attr("href");
            String title = link.text().trim();
            if (title.isBlank()) {
                title = href.startsWith("mailto:") ? "Email" : "Phone";
            }
            if (!seen.add(href)) {
                continue;
            }
            items.add(new ScrapedItem(
                    title,
                    null,
                    link.absUrl("href"),
                    null,
                    href.replace("mailto:", "").replace("tel:", ""),
                    Map.of("type", "contact", "channel", href.contains("mailto") ? "email" : "phone")
            ));
        }

        addFromLinks(document.select("a[href*='contact'], a[href*='support'], a[href*='help']"),
                items, seen, fallbackUrl, Map.of("type", "contact", "channel", "web"));

        return items;
    }

    private static List<ScrapedItem> parseNews(Document document, String fallbackUrl) {
        List<ScrapedItem> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        addFromLinks(document.select(
                "a[href*='news'], a[href*='blog'], a[href*='announcement'], a[href*='press'], article a"),
                items, seen, fallbackUrl, Map.of("type", "news"));

        addFromHeadings(document.select("article h2, article h3, h2, h3"), items, seen, fallbackUrl, Map.of("type", "news"));

        for (Element timeElement : document.select("time")) {
            String date = timeElement.attr("datetime");
            if (date.isBlank()) {
                date = timeElement.text().trim();
            }
            Element heading = timeElement.parent() != null
                    ? timeElement.parent().selectFirst("h2, h3, a")
                    : null;
            if (heading == null || date.isBlank() || !seen.add(heading.text() + date)) {
                continue;
            }
            items.add(new ScrapedItem(
                    heading.text().trim(),
                    date,
                    heading.tagName().equals("a") ? heading.absUrl("href") : fallbackUrl,
                    null,
                    date,
                    Map.of("type", "news", "field", "published-at")
            ));
        }

        return items;
    }

    private static void addFromLinks(
            Elements links,
            List<ScrapedItem> items,
            Set<String> seen,
            String fallbackUrl,
            Map<String, String> metadata) {
        for (Element link : links) {
            String title = link.text().trim();
            if (!isValidTitle(title) || !seen.add(title)) {
                continue;
            }
            items.add(new ScrapedItem(
                    title,
                    null,
                    link.absUrl("href").isBlank() ? fallbackUrl : link.absUrl("href"),
                    null,
                    null,
                    metadata
            ));
        }
    }

    private static void addFromHeadings(
            Elements headings,
            List<ScrapedItem> items,
            Set<String> seen,
            String fallbackUrl,
            Map<String, String> metadata) {
        for (Element heading : headings) {
            String title = heading.text().trim();
            if (!isValidTitle(title) || !seen.add(title)) {
                continue;
            }
            Element parentLink = heading.selectFirst("a[href]");
            String url = parentLink != null ? parentLink.absUrl("href") : fallbackUrl;
            items.add(new ScrapedItem(title, null, url, null, null, metadata));
        }
    }

    private static boolean isValidTitle(String title) {
        return !title.isBlank() && title.length() >= 4 && title.length() <= 200;
    }

    private static String metaContent(Document document, String name) {
        Element meta = document.selectFirst("meta[name=" + name + "], meta[property=og:" + name + "]");
        return meta != null ? meta.attr("content").trim() : null;
    }

    private static String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

}
