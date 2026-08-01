package com.datascraper.common.dto.messaging;

import com.datascraper.common.dto.discovery.DiscoveredCompany;

import java.util.List;
import java.util.UUID;

/**
 * Message published to the company enrichment queue after discovery.
 */
public record CompanyEnrichmentMessage(
        UUID jobId,
        String correlationId,
        DiscoveredCompany company,
        List<String> enabledProviders
) {
}
