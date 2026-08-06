/**
 * Pulls emails and phone numbers from contact page HTML.
 */
package com.datascraper.contact.support;

import com.datascraper.common.support.CompanyEmailSupport;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ContactHtmlParser {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(?:\\+\\d{1,3}[\\s.-]?)?(?:\\(?\\d{2,4}\\)?[\\s.-]?)?\\d{3,4}[\\s.-]?\\d{3,4}");

    public List<Map<String, Object>> parse(Document document, String sourceUrl, int maxItems) {
        return parse(document, sourceUrl, maxItems, null);
    }

    public List<Map<String, Object>> parse(Document document, String sourceUrl, int maxItems, String websiteUrl) {
        List<Map<String, Object>> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String site = websiteUrl != null && !websiteUrl.isBlank() ? websiteUrl : sourceUrl;

        List<String> emails = new ArrayList<>();
        for (Element link : document.select("a[href^=mailto:]")) {
            String email = CompanyEmailSupport.clean(link.attr("href"));
            if (email != null) {
                emails.add(email);
            }
        }
        // data-* / itemprop hints used by many CMS themes
        for (Element el : document.select("[data-email], [itemprop=email], [class*=email], [class*=Email]")) {
            String raw = firstNonBlank(el.attr("data-email"), el.attr("content"), el.text());
            for (String email : CompanyEmailSupport.extractFromText(raw)) {
                emails.add(email);
            }
        }
        emails.addAll(CompanyEmailSupport.extractFromText(document.html()));
        emails.addAll(CompanyEmailSupport.extractFromText(document.text()));

        for (String email : CompanyEmailSupport.rank(emails, site)) {
            addContact(items, seen, "email", email, sourceUrl);
        }

        for (Element link : document.select("a[href^=tel:]")) {
            String phone = link.attr("href").replace("tel:", "").trim();
            addContact(items, seen, "phone", phone, sourceUrl);
        }

        String pageText = document.text();
        Matcher phoneMatcher = PHONE_PATTERN.matcher(pageText);
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group().trim();
            if (looksLikeYearRange(phone)) {
                continue;
            }
            if (phone.replaceAll("\\D", "").length() >= 8) {
                addContact(items, seen, "phone", phone, sourceUrl);
            }
        }

        for (Element address : document.select("address")) {
            String text = address.text().trim();
            if (text.length() >= 12) {
                addContact(items, seen, "address", text, sourceUrl);
            }
        }

        return items.size() > maxItems ? items.subList(0, maxItems) : items;
    }

    private void addContact(
            List<Map<String, Object>> items,
            Set<String> seen,
            String field,
            String value,
            String sourceUrl
    ) {
        if (value == null || value.isBlank() || !seen.add(field + ":" + value.toLowerCase())) {
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("section", "contact");
        item.put("field", field);
        item.put("value", value);
        item.put("sourceUrl", sourceUrl);
        items.add(item);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean looksLikeYearRange(String value) {
        return value.matches("(?i)^(?:©\\s*)?19\\d{2}\\s*[-–—]\\s*20\\d{2}$")
                || value.matches("^19\\d{2}\\s*[-–—]\\s*20\\d{2}$");
    }
}
