package com.datascraper.orchestrator.controller;

import com.datascraper.orchestrator.dto.IntelligenceJobRequest;
import com.datascraper.orchestrator.dto.IntelligenceJobResponse;
import com.datascraper.orchestrator.service.IntelligenceOrchestratorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final IntelligenceOrchestratorService intelligenceOrchestratorService;

    public IntelligenceController(IntelligenceOrchestratorService intelligenceOrchestratorService) {
        this.intelligenceOrchestratorService = intelligenceOrchestratorService;
    }

    @PostMapping("/jobs")
    public ResponseEntity<IntelligenceJobResponse> createJob(
            @Valid @RequestBody IntelligenceJobRequest request,
            HttpServletRequest httpRequest
    ) {
        String correlationId = httpRequest.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "local-" + System.currentTimeMillis();
        }
        return ResponseEntity.ok(intelligenceOrchestratorService.runJob(request, correlationId));
    }
}
