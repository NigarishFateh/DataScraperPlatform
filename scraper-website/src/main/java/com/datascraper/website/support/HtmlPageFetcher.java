/**
 * Downloads a web page and returns it as HTML for parsing.
 */
package com.datascraper.website.support;

import com.datascraper.website.config.WebsiteScraperProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Downloads HTML over HTTP and builds a JSoup {@link Document} (DOM tree).
 */
@Component
public class HtmlPageFetcher {

    private final WebsiteScraperProperties properties;

    public HtmlPageFetcher(WebsiteScraperProperties properties) {
        this.properties = properties;
    }

    public Document fetch(String url) throws IOException {
        URI uri = URI.create(normalizeUrl(url));
        return Jsoup.connect(uri.toString())
                .userAgent(properties.getUserAgent())
                .timeout(properties.getTimeoutMs())
                .followRedirects(true)
                .get();
    }

    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }
}
