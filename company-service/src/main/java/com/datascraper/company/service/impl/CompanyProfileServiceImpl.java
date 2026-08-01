/**
 * Implements enriched profile upsert and export-oriented read APIs.
 */
package com.datascraper.company.service.impl;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.company.dto.EnrichedCompanyUpsertResponse;
import com.datascraper.company.entity.CompanyProfileEntity;
import com.datascraper.company.entity.NormalizationLogEntity;
import com.datascraper.company.exception.CompanyProfileNotFoundException;
import com.datascraper.company.mapper.CompanyProfileMapper;
import com.datascraper.company.repository.CompanyProfileJpaRepository;
import com.datascraper.company.repository.NormalizationLogJpaRepository;
import com.datascraper.company.service.CompanyProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CompanyProfileServiceImpl implements CompanyProfileService {

    private final CompanyProfileJpaRepository profileRepository;
    private final NormalizationLogJpaRepository normalizationLogRepository;
    private final CompanyProfileMapper profileMapper;

    public CompanyProfileServiceImpl(
            CompanyProfileJpaRepository profileRepository,
            NormalizationLogJpaRepository normalizationLogRepository,
            CompanyProfileMapper profileMapper
    ) {
        this.profileRepository = profileRepository;
        this.normalizationLogRepository = normalizationLogRepository;
        this.profileMapper = profileMapper;
    }

    @Override
    @Transactional
    public EnrichedCompanyUpsertResponse upsertEnriched(UUID jobId, EnrichedCompany company) {
        if (jobId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Job-Id header is required");
        }
        if (company.name() == null || company.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company name is required");
        }

        String normalizedKey = profileMapper.computeNormalizedKey(company);
        if (normalizedKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to derive normalized key from company name or website"
            );
        }

        CompanyProfileEntity entity = profileRepository
                .findByJobIdAndNormalizedKey(jobId, normalizedKey)
                .orElseGet(CompanyProfileEntity::new);

        boolean created = entity.getId() == null;
        Instant existingCreatedAt = entity.getCreatedAt();
        if (created) {
            entity.setId(resolveProfileId(company));
        }

        profileMapper.applyEnrichedCompany(entity, company, jobId, normalizedKey);

        Instant now = Instant.now();
        entity.setCreatedAt(created ? now : existingCreatedAt != null ? existingCreatedAt : now);
        entity.setUpdatedAt(now);

        CompanyProfileEntity saved = profileRepository.save(entity);
        writeNormalizationLog(jobId, saved.getId(), created ? "CREATE" : "UPDATE", normalizedKey);

        return new EnrichedCompanyUpsertResponse(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EnrichedCompany> findByJob(UUID jobId, int page, int pageSize) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(pageSize, 1), 500);

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.asc("name")));
        Page<CompanyProfileEntity> result = profileRepository.findByJobId(jobId, pageRequest);

        List<EnrichedCompany> items = result.getContent().stream()
                .map(profileMapper::toEnrichedCompany)
                .toList();

        return PageResponse.of(items, safePage, safeSize, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public EnrichedCompany getProfileById(String id) {
        return profileRepository.findById(id)
                .map(profileMapper::toEnrichedCompany)
                .orElseThrow(() -> new CompanyProfileNotFoundException(id));
    }

    private String resolveProfileId(EnrichedCompany company) {
        if (company.id() != null && !company.id().isBlank()) {
            return company.id().trim();
        }
        return "cp-" + UUID.randomUUID();
    }

    private void writeNormalizationLog(UUID jobId, String profileId, String action, String normalizedKey) {
        NormalizationLogEntity log = new NormalizationLogEntity();
        log.setId("nl-" + UUID.randomUUID());
        log.setJobId(jobId);
        log.setProfileId(profileId);
        log.setAction(action);
        log.setDetails("normalized_key=" + normalizedKey);
        normalizationLogRepository.save(log);
    }
}
