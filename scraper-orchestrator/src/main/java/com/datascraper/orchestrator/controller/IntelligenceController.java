/**
 * REST endpoint that starts an intelligence scrape job.
 */
package com.datascraper.orchestrator.controller;

import com.datascraper.orchestrator.dto.IntelligenceJobRequest;
import com.datascraper.orchestrator.dto.IntelligenceJobResponse;
import com.datascraper.orchestrator.service.EnrichmentResumeService;
import com.datascraper.orchestrator.service.IntelligenceOrchestratorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final IntelligenceOrchestratorService intelligenceOrchestratorService;
    private final EnrichmentResumeService enrichmentResumeService;

    public IntelligenceController(
            IntelligenceOrchestratorService intelligenceOrchestratorService,
            EnrichmentResumeService enrichmentResumeService
    ) {
        this.intelligenceOrchestratorService = intelligenceOrchestratorService;
        this.enrichmentResumeService = enrichmentResumeService;
    }

    /**
     * @deprecated Prefer {@code POST /api/orchestrator/enrich} for the async enrichment pipeline.
     */
    @Deprecated
    @PostMapping("/jobs")
    public ResponseEntity<IntelligenceJobResponse> createJob(
            @Valid @RequestBody IntelligenceJobRequest request,
            HttpServletRequest httpRequest
    ) {
        String correlationId = httpRequest.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "local-" + System.currentTimeMillis();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.WARNING, "299 - \"Deprecated: use POST /api/orchestrator/enrich\"")
                .body(intelligenceOrchestratorService.runJob(request, correlationId));
    }

    /**
     * @deprecated Prefer {@code POST /api/orchestrator/jobs/{jobId}/resume}.
     */
    @Deprecated
    @PostMapping("/jobs/{jobId}/resume")
    public ResponseEntity<?> resumeLegacy(@PathVariable UUID jobId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.WARNING, "299 - \"Deprecated: use POST /api/orchestrator/jobs/{jobId}/resume\"")
                .body(enrichmentResumeService.resume(jobId));
    }
}
