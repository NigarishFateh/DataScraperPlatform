package com.datascraper.microsoft.service.impl;

import com.datascraper.microsoft.dto.MicrosoftScrapeResponse;
import com.datascraper.microsoft.model.JobListing;
import com.datascraper.microsoft.service.MicrosoftScraperService;
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
import java.util.Set;

@Slf4j
@Service
public class MicrosoftScraperServiceImpl implements MicrosoftScraperService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final String careersUrl;
    private final int timeoutMs;

    public MicrosoftScraperServiceImpl(
            @Value("${scraper.microsoft.careers-url}") String careersUrl,
            @Value("${scraper.microsoft.timeout-ms}") int timeoutMs) {
        this.careersUrl = careersUrl;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public MicrosoftScrapeResponse scrapeJobs() {
        log.info("Starting Microsoft careers scrape from {}", careersUrl);

        try {
            Document document = downloadPage(careersUrl);
            List<JobListing> jobs = parseJobListings(document);

            log.info("Microsoft scrape completed. Found {} job listings.", jobs.size());

            return new MicrosoftScrapeResponse(
                    "microsoft",
                    Instant.now(),
                    document.title(),
                    jobs.size(),
                    jobs
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scrape Microsoft careers page", exception);
        }
    }

    private Document downloadPage(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(timeoutMs)
                .followRedirects(true)
                .get();
    }

    private List<JobListing> parseJobListings(Document document) {
        Set<String> seenTitles = new LinkedHashSet<>();
        List<JobListing> jobs = new ArrayList<>();

        extractJobsFromLinks(document, seenTitles, jobs);
        extractJobsFromHeadings(document, seenTitles, jobs);

        return jobs;
    }

    private void extractJobsFromLinks(Document document, Set<String> seenTitles, List<JobListing> jobs) {
        Elements jobLinks = document.select("a[href*='job'], a[href*='search'], a[href*='careers']");

        for (Element link : jobLinks) {
            String title = link.text().trim();
            if (title.isBlank() || title.length() < 4 || !seenTitles.add(title)) {
                continue;
            }

            jobs.add(new JobListing(
                    title,
                    "Not specified in page HTML",
                    link.absUrl("href")
            ));
        }
    }

    private void extractJobsFromHeadings(Document document, Set<String> seenTitles, List<JobListing> jobs) {
        Elements headings = document.select("h2, h3, h4");

        for (Element heading : headings) {
            String title = heading.text().trim();
            if (title.isBlank() || title.length() < 4 || !seenTitles.add(title)) {
                continue;
            }

            Element parentLink = heading.selectFirst("a[href]");
            String url = parentLink != null ? parentLink.absUrl("href") : careersUrl;

            jobs.add(new JobListing(
                    title,
                    "Not specified in page HTML",
                    url
            ));
        }
    }

}
