package com.datascraper.orchestrator.support;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.common.dto.provider.ProviderContext;

public final class ProviderContextMapper {

    private ProviderContextMapper() {
    }

    public static ProviderContext fromMessage(CompanyEnrichmentMessage message) {
        DiscoveredCompany company = message.company();
        String companyId = company.externalId() != null && !company.externalId().isBlank()
                ? company.externalId()
                : company.name();
        return new ProviderContext(
                message.jobId().toString(),
                companyId,
                company.name(),
                company.website(),
                company.countryCode(),
                company.cityName(),
                company.categoryIds(),
                message.correlationId()
        );
    }
}
