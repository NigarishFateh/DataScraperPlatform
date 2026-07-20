package com.datascraper.orchestrator.controller;

import com.datascraper.orchestrator.dto.ScrapeResponse;
import com.datascraper.orchestrator.service.OrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrchestratorController {

    private final OrchestratorService orchestratorService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "scraper-orchestrator",
                "status", "UP",
                "message", orchestratorService.getHealthStatus()
        ));
    }

    @PostMapping("/scrape")
    public ResponseEntity<ScrapeResponse> scrape() {
        return ResponseEntity.ok(orchestratorService.initiateScrape());
    }

}
