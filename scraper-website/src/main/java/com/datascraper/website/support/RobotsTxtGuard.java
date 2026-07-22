package com.datascraper.website.support;

import com.datascraper.website.config.WebsiteScraperProperties;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimal robots.txt guard — skips scrape when path is disallowed for {@code *}.
 * Production systems often use a dedicated crawler library; this teaches the concept.
 */
@Component
public class RobotsTxtGuard {

    private final WebsiteScraperProperties properties;

    public RobotsTxtGuard(WebsiteScraperProperties properties) {
        this.properties = properties;
    }

    public void verifyAllowed(String targetUrl) throws IOException {
        if (!properties.isRespectRobotsTxt()) {
            return;
        }
        URI uri = URI.create(targetUrl.startsWith("http") ? targetUrl : "https://" + targetUrl);
        String robotsUrl = uri.getScheme() + "://" + uri.getHost() + "/robots.txt";
        String body;
        try {
            body = Jsoup.connect(robotsUrl)
                    .userAgent(properties.getUserAgent())
                    .timeout(properties.getTimeoutMs())
                    .ignoreContentType(true)
                    .execute()
                    .body();
        } catch (IOException ex) {
            // No robots.txt or unreachable — proceed (common for small sites).
            return;
        }
        if (isPathDisallowed(body, uri.getPath())) {
            throw new IOException("robots.txt disallows scraping path: " + uri.getPath());
        }
    }

    static boolean isPathDisallowed(String robotsBody, String path) {
        if (robotsBody == null || robotsBody.isBlank()) {
            return false;
        }
        String normalizedPath = path == null || path.isBlank() ? "/" : path;
        List<String> disallows = new ArrayList<>();
        boolean starAgent = false;

        for (String rawLine : robotsBody.split("\n")) {
            String line = rawLine.split("#", 2)[0].trim();
            if (line.isEmpty()) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("user-agent:")) {
                String agent = line.substring("user-agent:".length()).trim();
                starAgent = agent.equals("*");
            } else if (starAgent && lower.startsWith("disallow:")) {
                String rule = line.substring("disallow:".length()).trim();
                if (!rule.isEmpty()) {
                    disallows.add(rule);
                }
            }
        }

        for (String rule : disallows) {
            if (rule.equals("/")) {
                return true;
            }
            if (!rule.isEmpty() && normalizedPath.startsWith(rule)) {
                return true;
            }
        }
        return false;
    }
}
