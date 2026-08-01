/**
 * Service contract for enriched company profile persistence and export queries.
 */
package com.datascraper.company.service;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.company.dto.EnrichedCompanyUpsertResponse;

import java.util.UUID;

public interface CompanyProfileService {

    EnrichedCompanyUpsertResponse upsertEnriched(UUID jobId, EnrichedCompany company);

    PageResponse<EnrichedCompany> findByJob(UUID jobId, int page, int pageSize);

    EnrichedCompany getProfileById(String id);
}
