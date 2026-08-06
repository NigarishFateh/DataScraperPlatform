package com.datascraper.orchestrator.controller;

import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.orchestrator.dto.EnrichmentProcessResponse;
import com.datascraper.orchestrator.model.EnrichmentProcessResult;
import com.datascraper.orchestrator.service.CompanyEnrichmentPipelineService;
import com.datascraper.orchestrator.service.EnrichmentResumeService;
import com.datascraper.orchestrator.support.EnrichmentConcurrencyGuard;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orchestrator")
public class OrchestratorController {

    private final CompanyEnrichmentPipelineService pipelineService;
    private final EnrichmentResumeService resumeService;
    private final EnrichmentConcurrencyGuard concurrencyGuard;

    public OrchestratorController(
            CompanyEnrichmentPipelineService pipelineService,
            EnrichmentResumeService resumeService,
            EnrichmentConcurrencyGuard concurrencyGuard
    ) {
        this.pipelineService = pipelineService;
        this.resumeService = resumeService;
        this.concurrencyGuard = concurrencyGuard;
    }

    @PostMapping("/enrich")
    public ResponseEntity<EnrichmentProcessResponse> enrich(@Valid @RequestBody CompanyEnrichmentMessage message) {
        EnrichmentProcessResult result = concurrencyGuard.run(() -> pipelineService.process(message));
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/jobs/{jobId}/resume")
    public ResponseEntity<JobResponse> resume(@PathVariable UUID jobId) {
        return ResponseEntity.ok(resumeService.resume(jobId));
    }

    private EnrichmentProcessResponse toResponse(EnrichmentProcessResult result) {
        return new EnrichmentProcessResponse(
                result.jobId(),
                result.companyId(),
                result.enrichedCompany(),
                result.providerResults(),
                result.validation(),
                result.persisted(),
                result.checkpoint()
        );
    }
}
