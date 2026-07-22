package com.datascraper.contact.support;

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

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(?:\\+\\d{1,3}[\\s.-]?)?(?:\\(?\\d{2,4}\\)?[\\s.-]?)?\\d{3,4}[\\s.-]?\\d{3,4}");

    public List<Map<String, Object>> parse(Document document, String sourceUrl, int maxItems) {
        List<Map<String, Object>> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (Element link : document.select("a[href^=mailto:]")) {
            String email = link.attr("href").replace("mailto:", "").split("\\?")[0].trim();
            addContact(items, seen, "email", email, sourceUrl);
        }

        for (Element link : document.select("a[href^=tel:]")) {
            String phone = link.attr("href").replace("tel:", "").trim();
            addContact(items, seen, "phone", phone, sourceUrl);
        }

        String pageText = document.text();
        Matcher emailMatcher = EMAIL_PATTERN.matcher(pageText);
        while (emailMatcher.find()) {
            addContact(items, seen, "email", emailMatcher.group(), sourceUrl);
        }

        Matcher phoneMatcher = PHONE_PATTERN.matcher(pageText);
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group().trim();
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
}
