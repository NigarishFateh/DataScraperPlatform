/**
 * Downloads an IBM page and builds scrape results for a category.
 */
package com.datascraper.ibm.service.impl;

import com.datascraper.ibm.config.IbmUrlProperties;
import com.datascraper.ibm.model.DataCategory;
import com.datascraper.ibm.model.ScrapedData;
import com.datascraper.ibm.model.ScrapedItem;
import com.datascraper.ibm.service.IbmScraperService;
import com.datascraper.ibm.support.HtmlScrapeParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IbmScraperServiceImpl implements IbmScraperService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final IbmUrlProperties urlProperties;
    private final int timeoutMs;

    public IbmScraperServiceImpl(
            IbmUrlProperties urlProperties,
            @Value("${scraper.ibm.timeout-ms}") int timeoutMs) {
        this.urlProperties = urlProperties;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public ScrapedData scrape(DataCategory category) {
        String targetUrl = urlProperties.urlFor(category);
        log.info("Starting IBM {} scrape from {}", category, targetUrl);

        try {
            Document document = downloadPage(targetUrl);
            List<ScrapedItem> items = HtmlScrapeParser.parse(document, category, targetUrl);

            log.info("IBM {} scrape completed. Found {} items.", category, items.size());

            return new ScrapedData(
                    "ibm",
                    category,
                    Instant.now(),
                    document.title(),
                    items.size(),
                    items,
                    Map.of("status", "SUCCESS", "targetUrl", targetUrl)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scrape IBM " + category + " page", exception);
        }
    }

    private Document downloadPage(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(timeoutMs)
                .followRedirects(true)
                .get();
    }

}
