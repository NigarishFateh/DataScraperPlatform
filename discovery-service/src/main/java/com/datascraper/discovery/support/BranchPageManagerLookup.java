package com.datascraper.discovery.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort store-page parse for a branch manager. Never copies brand CEO.
 * Maps URLs and failures return null so the Excel cell stays empty.
 */
@Component
public class BranchPageManagerLookup {

    private static final Logger log = LoggerFactory.getLogger(BranchPageManagerLookup.class);
    private static final Pattern LABELED = Pattern.compile(
            "(?i)\\b(vestigingsmanager|filiaalmanager|winkelmanager|filiaalhouder|"
                    + "vestigingsdirecteur|store manager|branch manager|restaurant manager|"
                    + "restaurantmanager)\\b\\s*[:\\-|–]?\\s*"
                    + "([A-Z][a-zA-Z''\\-]+(?:\\s+(?:van|de|den|der|ten|ter)\\s+[A-Z][a-zA-Z''\\-]+)?"
                    + "(?:\\s+[A-Z][a-zA-Z''\\-]+){0,3})"
    );
    private static final Pattern NAMED_THEN_ROLE = Pattern.compile(
            "(?i)\\b([A-Z][a-zA-Z''\\-]+(?:\\s+(?:van|de|den|der|ten|ter)\\s+[A-Z][a-zA-Z''\\-]+)?"
                    + "(?:\\s+[A-Z][a-zA-Z''\\-]+){0,3})\\s*[,\\-|–]\\s*"
                    + "(vestigingsmanager|filiaalmanager|winkelmanager|filiaalhouder|"
                    + "vestigingsdirecteur|store manager|branch manager|restaurant manager|"
                    + "restaurantmanager)\\b"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String lookup(String pageUrl) {
        if (!WebsiteUrlSupport.isUsableCompanyWebsite(pageUrl)) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(pageUrl.trim()))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "DataScraperPlatform/0.1")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                return null;
            }
            String text = stripHtml(response.body());
            String fromLabeled = firstPerson(LABELED.matcher(text), 2);
            if (fromLabeled != null) {
                return fromLabeled;
            }
            return firstPerson(NAMED_THEN_ROLE.matcher(text), 1);
        } catch (Exception ex) {
            log.debug("Branch manager lookup skipped for {}: {}", pageUrl, ex.getMessage());
            return null;
        }
    }

    private static String firstPerson(Matcher matcher, int nameGroup) {
        while (matcher.find()) {
            String person = cleanPersonName(matcher.group(nameGroup));
            if (person != null) {
                return person;
            }
        }
        return null;
    }

    private static String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?i)&nbsp;?", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ");
    }

    private static String cleanPersonName(String raw) {
        if (raw == null) {
            return null;
        }
        String name = raw.replaceAll("\\s+", " ").trim();
        if (name.length() < 3 || name.length() > 60) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("company") || lower.contains("limited") || lower.contains("contact")
                || lower.contains("email") || lower.contains("team")) {
            return null;
        }
        return name;
    }
}
