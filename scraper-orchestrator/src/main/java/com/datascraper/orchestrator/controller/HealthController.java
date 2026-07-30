/**
 * Simple health endpoint for platform probes and local startup checks.
 */
package com.datascraper.orchestrator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "scraper-orchestrator",
                "status", "UP",
                "message", "Intelligence orchestrator is running"
        ));
    }
}
