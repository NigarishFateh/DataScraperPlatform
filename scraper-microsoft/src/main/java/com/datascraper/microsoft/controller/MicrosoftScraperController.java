package com.datascraper.microsoft.controller;

import com.datascraper.microsoft.dto.MicrosoftScrapeResponse;
import com.datascraper.microsoft.service.MicrosoftScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MicrosoftScraperController {

    private final MicrosoftScraperService microsoftScraperService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "scraper-microsoft",
                "status", "UP",
                "message", "Microsoft scraper microservice is running."
        ));
    }

    @GetMapping("/scrape/jobs")
    public ResponseEntity<MicrosoftScrapeResponse> scrapeJobs() {
        return ResponseEntity.ok(microsoftScraperService.scrapeJobs());
    }

}
