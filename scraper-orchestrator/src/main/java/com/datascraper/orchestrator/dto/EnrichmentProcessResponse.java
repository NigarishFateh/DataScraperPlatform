package com.datascraper.orchestrator.dto;

import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.common.dto.provider.ProviderResult;
import com.datascraper.orchestrator.model.ValidationOutcome;

import java.util.List;
import java.util.UUID;

public record EnrichmentProcessResponse(
        UUID jobId,
        String companyId,
        EnrichedCompany enrichedCompany,
        List<ProviderResult> providerResults,
        ValidationOutcome validation,
        boolean persisted,
        String checkpoint
) {
}
