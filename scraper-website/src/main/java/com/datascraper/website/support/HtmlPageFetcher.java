/**
 * Downloads a web page and returns it as HTML for parsing.
 */
package com.datascraper.website.support;

import com.datascraper.website.config.WebsiteScraperProperties;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Locale;

@Component
public class HtmlPageFetcher {

    private final WebsiteScraperProperties properties;
    private final HttpClient httpClient;

    public HtmlPageFetcher(WebsiteScraperProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Document fetch(String url) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IOException("websiteUrl is required");
        }
        URI uri = URI.create(normalizeUrl(url));
        Duration timeout = Duration.ofMillis(Math.max(3_000, properties.getTimeoutMs()));

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,nl;q=0.8")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 400) {
                throw new HttpStatusException("HTTP " + status + " fetching URL", status, uri.toString());
            }
            String body = response.body() == null ? "" : response.body();
            if (body.length() > 2 * 1024 * 1024) {
                body = body.substring(0, 2 * 1024 * 1024);
            }
            return Jsoup.parse(body, uri.toString());
        } catch (HttpTimeoutException ex) {
            throw new SocketTimeoutException("Read timed out fetching " + uri);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Fetch interrupted for " + uri, ex);
        }
    }

    public static boolean isBlockedOrUnavailable(IOException ex) {
        if (ex instanceof SocketTimeoutException || ex instanceof HttpTimeoutException) {
            return true;
        }
        if (ex instanceof HttpStatusException http) {
            int status = http.getStatusCode();
            return status == 401 || status == 403 || status == 407 || status == 429
                    || status == 503 || status == 520 || status == 521 || status == 522 || status == 523 || status == 524;
        }
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
        return message.contains("timed out")
                || message.contains("timeout")
                || message.contains("401")
                || message.contains("403")
                || message.contains("429")
                || message.contains("connection reset")
                || message.contains("handshake");
    }

    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }
}
