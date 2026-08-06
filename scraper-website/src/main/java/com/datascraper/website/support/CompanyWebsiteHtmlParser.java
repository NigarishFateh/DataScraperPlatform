/**
 * Pulls contact and founder fields from public company website HTML.
 */
package com.datascraper.website.support;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        addFounders(items, seen, document, sourceUrl);
        addContactChannels(items, seen, document);

        return items.size() > maxItems ? items.subList(0, maxItems) : items;
    }

    private void addFounders(
            List<Map<String, Object>> items,
            Set<String> seen,
            Document document,
            String sourceUrl
    ) {
        Pattern labeled = Pattern.compile(
                "(?i)\\b(founder|co[-\\s]?founder|ceo|owner|proprietor|managing director|directeur|oprichter|eigenaar|zaakvoerder)\\b\\s*[:\\-|–]?\\s*([A-Z][a-zA-Z''\\-]+(?:\\s+[A-Z][a-zA-Z''\\-]+){0,3})"
        );
        Pattern namedThenRole = Pattern.compile(
                "(?i)\\b([A-Z][a-zA-Z''\\-]+(?:\\s+[A-Z][a-zA-Z''\\-]+){0,3})\\s*[,\\-|–]\\s*(founder|co[-\\s]?founder|ceo|owner|proprietor|managing director|directeur|oprichter|eigenaar)\\b"
        );

        String pageText = document.text();
        Matcher labeledMatcher = labeled.matcher(pageText);
        while (labeledMatcher.find()) {
            String role = labeledMatcher.group(1).toLowerCase(Locale.ROOT);
            String person = cleanPersonName(labeledMatcher.group(2));
            if (person == null) {
                continue;
            }
            String field = role.contains("ceo") || role.contains("directeur") ? "ceo" : "founder";
            if (!seen.add(field + ":" + person.toLowerCase(Locale.ROOT))) {
                continue;
            }
            items.add(item("people", field, person, role, sourceUrl, null));
        }

        Matcher namedMatcher = namedThenRole.matcher(pageText);
        while (namedMatcher.find()) {
            String person = cleanPersonName(namedMatcher.group(1));
            String role = namedMatcher.group(2).toLowerCase(Locale.ROOT);
            if (person == null) {
                continue;
            }
            String field = role.contains("ceo") || role.contains("directeur") ? "ceo" : "founder";
            if (!seen.add(field + ":" + person.toLowerCase(Locale.ROOT))) {
                continue;
            }
            items.add(item("people", field, person, role, sourceUrl, null));
        }
    }

    private static String cleanPersonName(String raw) {
        if (raw == null) {
            return null;
        }
        String name = raw.replaceAll("\\s+", " ").trim();
        String[] parts = name.split("\\s+");
        StringJoiner joiner = new StringJoiner(" ");
        for (String part : parts) {
            String token = part.toLowerCase(Locale.ROOT);
            if (NON_NAME_TOKENS.contains(token)) {
                break;
            }
            joiner.add(part);
        }
        name = joiner.toString().trim();
        if (name.length() < 3 || name.length() > 60) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("company") || lower.contains("limited") || lower.contains("bv")
                || lower.contains("inc") || lower.contains("llc") || lower.contains("our team")) {
            return null;
        }
        return name;
    }

    private static final Set<String> NON_NAME_TOKENS = Set.of(
            "email", "call", "phone", "contact", "linkedin", "twitter", "facebook", "website", "us", "the"
    );

    private void addMeta(List<Map<String, Object>> items, Set<String> seen, Document document, String sourceUrl) {
        putField(items, seen, "identity", "pageTitle", document.title(), sourceUrl);
        putField(items, seen, "identity", "ogTitle", metaContent(document, "og:title"), sourceUrl);
        putField(items, seen, "identity", "ogSiteName", metaContent(document, "og:site_name"), sourceUrl);

        Element canonical = document.selectFirst("link[rel=canonical]");
        if (canonical != null) {
            putField(items, seen, "identity", "canonicalUrl", canonical.attr("href"), sourceUrl);
        }
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
}
