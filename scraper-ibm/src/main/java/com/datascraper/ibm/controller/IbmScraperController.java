/**
 * Exposes health and scrape HTTP endpoints for IBM pages.
 */
package com.datascraper.ibm.controller;

import com.datascraper.ibm.model.DataCategory;
import com.datascraper.ibm.model.ScrapedData;
import com.datascraper.ibm.service.IbmScraperService;
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
public class IbmScraperController {

    private final IbmScraperService ibmScraperService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "scraper-ibm",
                "status", "UP",
                "message", "IBM scraper microservice is running."
        ));
    }

    @GetMapping("/scrape/{category}")
    public ResponseEntity<ScrapedData> scrape(@PathVariable String category) {
        DataCategory dataCategory = DataCategory.fromPath(category);
        return ResponseEntity.ok(ibmScraperService.scrape(dataCategory));
    }

}
