package com.datascraper.google.controller;

import com.datascraper.google.model.DataCategory;
import com.datascraper.google.model.ScrapedData;
import com.datascraper.google.service.GoogleScraperService;
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
public class GoogleScraperController {

    private final GoogleScraperService googleScraperService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "scraper-google",
                "status", "UP",
                "message", "Google scraper microservice is running."
        ));
    }

    @GetMapping("/scrape/{category}")
    public ResponseEntity<ScrapedData> scrape(@PathVariable String category) {
        DataCategory dataCategory = DataCategory.fromPath(category);
        return ResponseEntity.ok(googleScraperService.scrape(dataCategory));
    }

}
