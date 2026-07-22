package com.datascraper.tech.support;

import com.datascraper.tech.config.TechScraperProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class HtmlPageFetcher {

    private final TechScraperProperties properties;

    public HtmlPageFetcher(TechScraperProperties properties) {
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
