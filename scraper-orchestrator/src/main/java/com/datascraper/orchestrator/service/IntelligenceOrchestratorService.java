package com.datascraper.orchestrator.service;

import com.datascraper.orchestrator.dto.IntelligenceJobRequest;
import com.datascraper.orchestrator.dto.IntelligenceJobResponse;

public interface IntelligenceOrchestratorService {

    IntelligenceJobResponse runJob(IntelligenceJobRequest request, String correlationId);
}
