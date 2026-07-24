/**
 * Exposes health and scrape HTTP endpoints for Microsoft pages.
 */
package com.datascraper.microsoft.controller;

import com.datascraper.microsoft.model.DataCategory;
import com.datascraper.microsoft.model.ScrapedData;
import com.datascraper.microsoft.service.MicrosoftScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/scrape/{category}")
    public ResponseEntity<ScrapedData> scrape(@PathVariable String category) {
        DataCategory dataCategory = DataCategory.fromPath(category);
        return ResponseEntity.ok(microsoftScraperService.scrape(dataCategory));
    }

}
