package com.datascraper.orchestrator.model;

import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.common.dto.provider.ProviderResult;

import java.util.List;
import java.util.UUID;

public record EnrichmentProcessResult(
        UUID jobId,
        String companyId,
        EnrichedCompany enrichedCompany,
        List<ProviderResult> providerResults,
        ValidationOutcome validation,
        boolean persisted,
        String checkpoint
) {
}
