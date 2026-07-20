package com.datascraper.google.service.impl;

import com.datascraper.google.model.DataCategory;
import com.datascraper.google.model.ScrapedData;
import com.datascraper.google.model.ScrapedItem;
import com.datascraper.google.service.GoogleScraperService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class GoogleScraperServiceImpl implements GoogleScraperService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final String careersUrl;
    private final int timeoutMs;

    public GoogleScraperServiceImpl(
            @Value("${scraper.google.careers-url}") String careersUrl,
            @Value("${scraper.google.timeout-ms}") int timeoutMs) {
        this.careersUrl = careersUrl;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public ScrapedData scrape(DataCategory category) {
        return switch (category) {
            case JOBS -> scrapeJobs();
            case PRODUCTS, SERVICES, COMPANY_INFO, CONTACTS, NEWS ->
                    emptyResult(category, "Category not yet implemented for Google scraper.");
        };
    }

    private ScrapedData scrapeJobs() {
        log.info("Starting Google careers scrape from {}", careersUrl);

        try {
            Document document = downloadPage(careersUrl);
            List<ScrapedItem> items = parseJobItems(document);

            log.info("Google scrape completed. Found {} job listings.", items.size());

            return new ScrapedData(
                    "google",
                    DataCategory.JOBS,
                    Instant.now(),
                    document.title(),
                    items.size(),
                    items,
                    Map.of("status", "SUCCESS")
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scrape Google careers page", exception);
        }
    }

    private ScrapedData emptyResult(DataCategory category, String message) {
        return new ScrapedData(
                "google",
                category,
                Instant.now(),
                message,
                0,
                List.of(),
                Map.of("status", "NOT_IMPLEMENTED")
        );
    }

    private Document downloadPage(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(timeoutMs)
                .followRedirects(true)
                .get();
    }

    private List<ScrapedItem> parseJobItems(Document document) {
        Set<String> seenTitles = new LinkedHashSet<>();
        List<ScrapedItem> items = new ArrayList<>();

        extractItemsFromLinks(document, seenTitles, items);
        extractItemsFromHeadings(document, seenTitles, items);

        return items;
    }

    private void extractItemsFromLinks(Document document, Set<String> seenTitles, List<ScrapedItem> items) {
        Elements jobLinks = document.select("a[href*='jobs/results'], a[href*='jobdetails']");

        for (Element link : jobLinks) {
            String title = link.text().trim();
            if (title.isBlank() || title.length() < 4 || !seenTitles.add(title)) {
                continue;
            }

            items.add(toJobItem(title, "Not specified in page HTML", link.absUrl("href")));
        }
    }

    private void extractItemsFromHeadings(Document document, Set<String> seenTitles, List<ScrapedItem> items) {
        Elements headings = document.select("h2, h3, h4");

        for (Element heading : headings) {
            String title = heading.text().trim();
            if (title.isBlank() || title.length() < 4 || !seenTitles.add(title)) {
                continue;
            }

            Element parentLink = heading.selectFirst("a[href]");
            String url = parentLink != null ? parentLink.absUrl("href") : careersUrl;

            items.add(toJobItem(title, "Not specified in page HTML", url));
        }
    }

    private ScrapedItem toJobItem(String title, String location, String url) {
        return new ScrapedItem(
                title,
                null,
                url,
                location,
                null,
                Map.of("employmentType", "unknown")
        );
    }

}
