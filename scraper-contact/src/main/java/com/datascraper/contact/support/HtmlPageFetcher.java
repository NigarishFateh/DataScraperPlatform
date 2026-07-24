/**
 * Downloads a web page and returns it as HTML for contact parsing.
 */
package com.datascraper.contact.support;

import com.datascraper.contact.config.ContactScraperProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class HtmlPageFetcher {

    private final ContactScraperProperties properties;

    public HtmlPageFetcher(ContactScraperProperties properties) {
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
