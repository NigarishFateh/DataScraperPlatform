package com.datascraper.website.support;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses public company website HTML using CSS selectors (JSoup DOM API).
 */
@Component
public class CompanyWebsiteHtmlParser {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(?:\\+\\d{1,3}[\\s.-]?)?(?:\\(?\\d{2,4}\\)?[\\s.-]?)?\\d{3,4}[\\s.-]?\\d{3,4}");

    public List<Map<String, Object>> parse(Document document, String sourceUrl, int maxItems) {
        List<Map<String, Object>> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        addMeta(items, seen, document, sourceUrl);
        addHeadings(items, seen, document, sourceUrl);
        addParagraphs(items, seen, document, sourceUrl, maxItems);
        addOfferingLinks(items, seen, document);
        addCareerLinks(items, seen, document);
        addSocialLinks(items, seen, document);
        addContactChannels(items, seen, document);

        return items.size() > maxItems ? items.subList(0, maxItems) : items;
    }

    private void addMeta(List<Map<String, Object>> items, Set<String> seen, Document document, String sourceUrl) {
        putField(items, seen, "identity", "pageTitle", document.title(), sourceUrl);
        putField(items, seen, "identity", "metaDescription", metaContent(document, "description"), sourceUrl);
        putField(items, seen, "identity", "ogTitle", metaContent(document, "og:title"), sourceUrl);
        putField(items, seen, "identity", "ogDescription", metaContent(document, "og:description"), sourceUrl);
        putField(items, seen, "identity", "ogSiteName", metaContent(document, "og:site_name"), sourceUrl);

        Element canonical = document.selectFirst("link[rel=canonical]");
        if (canonical != null) {
            putField(items, seen, "identity", "canonicalUrl", canonical.attr("href"), sourceUrl);
        }
    }

    private void addHeadings(List<Map<String, Object>> items, Set<String> seen, Document document, String sourceUrl) {
        for (Element heading : document.select("h1, h2")) {
            String text = heading.text().trim();
            if (!isUsefulText(text) || !seen.add("heading:" + text)) {
                continue;
            }
            items.add(item("positioning", "heading", text, null, sourceUrl, heading.tagName()));
        }
    }

    private void addParagraphs(
            List<Map<String, Object>> items,
            Set<String> seen,
            Document document,
            String sourceUrl,
            int maxItems
    ) {
        for (Element paragraph : document.select("main p, article p, section p, p")) {
            String text = paragraph.text().trim();
            if (text.length() < 80 || !seen.add("p:" + text)) {
                continue;
            }
            items.add(item("positioning", "paragraph", truncate(text, 120), text, sourceUrl, null));
            if (items.size() >= maxItems) {
                return;
            }
        }
    }

    private void addOfferingLinks(List<Map<String, Object>> items, Set<String> seen, Document document) {
        Elements links = document.select(
                "a[href*='service'], a[href*='services'], a[href*='product'], a[href*='products'], a[href*='solution']");
        addLinkItems(items, seen, links, "offerings", "service-or-product-link");
    }

    private void addCareerLinks(List<Map<String, Object>> items, Set<String> seen, Document document) {
        Elements links = document.select(
                "a[href*='career'], a[href*='careers'], a[href*='jobs'], a[href*='join-us']");
        addLinkItems(items, seen, links, "talent", "careers-link");
    }

    private void addSocialLinks(List<Map<String, Object>> items, Set<String> seen, Document document) {
        addLinkItems(items, seen, document.select("a[href*='linkedin.com']"), "presence", "linkedin");
        addLinkItems(items, seen, document.select("a[href*='github.com']"), "presence", "github");
        addLinkItems(items, seen, document.select("a[href*='twitter.com'], a[href*='x.com']"), "presence", "twitter");
        addLinkItems(items, seen, document.select("a[href*='facebook.com']"), "presence", "facebook");
        addLinkItems(items, seen, document.select("a[href*='youtube.com']"), "presence", "youtube");
    }

    private void addContactChannels(List<Map<String, Object>> items, Set<String> seen, Document document) {
        for (Element link : document.select("a[href^=mailto:]")) {
            String email = link.attr("href").replace("mailto:", "").split("\\?")[0].trim();
            if (email.isBlank() || !seen.add("email:" + email.toLowerCase(Locale.ROOT))) {
                continue;
            }
            items.add(item("contact", "email", email, null, "mailto:" + email, null));
        }
        for (Element link : document.select("a[href^=tel:]")) {
            String phone = link.attr("href").replace("tel:", "").trim();
            if (phone.isBlank() || !seen.add("phone:" + phone)) {
                continue;
            }
            items.add(item("contact", "phone", phone, null, "tel:" + phone, null));
        }

        String pageText = document.text();
        Matcher emailMatcher = EMAIL_PATTERN.matcher(pageText);
        while (emailMatcher.find()) {
            String email = emailMatcher.group().trim();
            if (!seen.add("email:" + email.toLowerCase(Locale.ROOT))) {
                continue;
            }
            items.add(item("contact", "email", email, null, "mailto:" + email, null));
        }
        Matcher phoneMatcher = PHONE_PATTERN.matcher(pageText);
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group().trim();
            if (phone.matches("^19\\d{2}\\s*[-–—]\\s*20\\d{2}$")) {
                continue;
            }
            if (phone.replaceAll("\\D", "").length() < 8 || !seen.add("phone:" + phone)) {
                continue;
            }
            items.add(item("contact", "phone", phone, null, "tel:" + phone.replaceAll("[^\\d+]", ""), null));
        }
        for (Element address : document.select("address")) {
            String text = address.text().trim();
            if (text.length() < 12 || !seen.add("address:" + text.toLowerCase(Locale.ROOT))) {
                continue;
            }
            items.add(item("contact", "address", text, null, null, null));
        }
    }

    private void addLinkItems(
            List<Map<String, Object>> items,
            Set<String> seen,
            Elements links,
            String section,
            String field
    ) {
        for (Element link : links) {
            String title = link.text().trim();
            String href = link.absUrl("href");
            if (title.length() < 3 || href.isBlank() || !seen.add(field + ":" + href)) {
                continue;
            }
            items.add(item(section, field, title, null, href, null));
        }
    }

    private void putField(
            List<Map<String, Object>> items,
            Set<String> seen,
            String section,
            String field,
            String value,
            String sourceUrl
    ) {
        if (value == null || value.isBlank() || !seen.add(field + ":" + value)) {
            return;
        }
        items.add(item(section, field, value, null, sourceUrl, null));
    }

    private Map<String, Object> item(
            String section,
            String field,
            String title,
            String description,
            String url,
            String tag
    ) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("section", section);
        map.put("field", field);
        map.put("title", title);
        if (description != null) {
            map.put("description", description);
        }
        if (url != null) {
            map.put("url", url);
        }
        if (tag != null) {
            map.put("tag", tag);
        }
        return map;
    }

    private static String metaContent(Document document, String key) {
        Element meta = document.selectFirst(
                "meta[name=" + key + "], meta[property=" + key + "], meta[name=" + key.toLowerCase(Locale.ROOT) + "]");
        return meta != null ? meta.attr("content").trim() : null;
    }

    private static boolean isUsefulText(String text) {
        return text.length() >= 4 && text.length() <= 200;
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
