/**
 * Checks robots.txt to see if scraping a URL is allowed.
 */
package com.datascraper.website.support;

import com.datascraper.website.config.WebsiteScraperProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal robots.txt guard — skips scrape when path is disallowed for {@code *}.
 * Fetches with Java HttpClient (Jsoup connect timeouts often never fire) and caches per host.
 */
@Component
public class RobotsTxtGuard {

    private final WebsiteScraperProperties properties;
    private final HttpClient httpClient;
    private final ConcurrentHashMap<String, String> robotsByHost = new ConcurrentHashMap<>();

    public RobotsTxtGuard(WebsiteScraperProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void verifyAllowed(String targetUrl) throws IOException {
        if (!properties.isRespectRobotsTxt()) {
            return;
        }
        URI uri = URI.create(targetUrl.startsWith("http") ? targetUrl : "https://" + targetUrl);
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return;
        }
        String body = robotsByHost.computeIfAbsent(host.toLowerCase(Locale.ROOT), ignored -> fetchRobots(uri));
        if (body == null || body.isBlank()) {
            return;
        }
        if (isPathDisallowed(body, uri.getPath())) {
            throw new IOException("robots.txt disallows scraping path: " + uri.getPath());
        }
    }

    private String fetchRobots(URI pageUri) {
        try {
            String robotsUrl = pageUri.getScheme() + "://" + pageUri.getHost() + "/robots.txt";
            HttpRequest request = HttpRequest.newBuilder(URI.create(robotsUrl))
                    .timeout(Duration.ofMillis(Math.max(3_000, properties.getTimeoutMs())))
                    .header("User-Agent", properties.getUserAgent())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return "";
            }
            return response.body() == null ? "" : response.body();
        } catch (Exception ex) {
            return "";
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
