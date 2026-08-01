package com.datascraper.orchestrator.service;

import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.common.dto.provider.ProviderContext;
import com.datascraper.common.dto.provider.ProviderResult;
import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;
import com.datascraper.orchestrator.aggregation.AggregationService;
import com.datascraper.orchestrator.client.JobServiceClient;
import com.datascraper.orchestrator.client.PersistenceClient;
import com.datascraper.orchestrator.job.JobCompletionTracker;
import com.datascraper.orchestrator.model.CompanyDraft;
import com.datascraper.orchestrator.model.EnrichmentProcessResult;
import com.datascraper.orchestrator.model.ValidationOutcome;
import com.datascraper.orchestrator.normalization.NormalizationService;
import com.datascraper.orchestrator.support.ProviderContextMapper;
import com.datascraper.orchestrator.validation.ValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CompanyEnrichmentPipelineService {

    private final EnrichmentService enrichmentService;
    private final AggregationService aggregationService;
    private final NormalizationService normalizationService;
    private final ValidationService validationService;
    private final PersistenceClient persistenceClient;
    private final JobServiceClient jobServiceClient;
    private final JobCompletionTracker jobCompletionTracker;
    private final ObjectMapper objectMapper;

    public CompanyEnrichmentPipelineService(
            EnrichmentService enrichmentService,
            AggregationService aggregationService,
            NormalizationService normalizationService,
            ValidationService validationService,
            PersistenceClient persistenceClient,
            JobServiceClient jobServiceClient,
            JobCompletionTracker jobCompletionTracker,
            ObjectMapper objectMapper
    ) {
        this.enrichmentService = enrichmentService;
        this.aggregationService = aggregationService;
        this.normalizationService = normalizationService;
        this.validationService = validationService;
        this.persistenceClient = persistenceClient;
        this.jobServiceClient = jobServiceClient;
        this.jobCompletionTracker = jobCompletionTracker;
        this.objectMapper = objectMapper;
    }

    public EnrichmentProcessResult process(CompanyEnrichmentMessage message) {
        UUID jobId = message.jobId();
        if (shouldSkipPausedJob(jobId)) {
            log.info("Skipping enrichment for paused job {}", jobId);
            return skippedResult(message, "Job is paused");
        }

        ProviderContext context = ProviderContextMapper.fromMessage(message);
        List<ProviderResult> providerResults = enrichmentService.enrich(context, message.enabledProviders());

        CompanyDraft draft = aggregationService.aggregate(message.company(), providerResults);
        normalizationService.normalize(draft);
        ValidationOutcome validation = validationService.validate(draft);

        EnrichedCompany enrichedCompany = toEnrichedCompany(draft);
        boolean persisted = persistenceClient.persist(jobId, enrichedCompany);

        JobResponse currentJob = jobServiceClient.getJob(jobId);
        int enrichedCount = (currentJob != null ? currentJob.enrichedCount() : 0) + 1;
        int persistedCount = (currentJob != null ? currentJob.persistedCount() : 0) + (persisted ? 1 : 0);
        int failedCount = (currentJob != null ? currentJob.failedCount() : 0) + (validation.softFailure() ? 1 : 0);
        String checkpoint = buildCheckpoint(message.company());

        jobServiceClient.patchProgress(jobServiceClient.enrichmentProgress(
                jobId,
                currentJob,
                enrichedCount,
                persistedCount,
                failedCount,
                "Enriched company " + context.companyId(),
                checkpoint
        ));

        jobCompletionTracker.checkAndTriggerExport(jobId, enrichedCount);

        return new EnrichmentProcessResult(
                jobId,
                context.companyId(),
                enrichedCompany,
                providerResults,
                validation,
                persisted,
                checkpoint
        );
    }

    private boolean shouldSkipPausedJob(UUID jobId) {
        JobResponse job = jobServiceClient.getJob(jobId);
        return job != null && job.status() == JobStatus.PAUSED;
    }

    private EnrichmentProcessResult skippedResult(CompanyEnrichmentMessage message, String reason) {
        return new EnrichmentProcessResult(
                message.jobId(),
                message.company().externalId(),
                null,
                List.of(),
                new ValidationOutcome(false, true, false, List.of(reason)),
                false,
                buildCheckpoint(message.company())
        );
    }

    private EnrichedCompany toEnrichedCompany(CompanyDraft draft) {
        return new EnrichedCompany(
                draft.getId(),
                draft.getName(),
                draft.getCategory(),
                draft.getIndustry(),
                draft.getCountryCode(),
                draft.getCountryName(),
                draft.getState(),
                draft.getCity(),
                draft.getWebsite(),
                draft.getEmail(),
                draft.getPhone(),
                draft.getFounder(),
                draft.getCeo(),
                draft.getDescription(),
                draft.getServices(),
                draft.getProducts(),
                List.copyOf(draft.getTechnologyStack()),
                draft.getLinkedIn(),
                draft.getGithub(),
                draft.getFacebook(),
                draft.getTwitter(),
                draft.getInstagram(),
                draft.getYoutube(),
                draft.getFoundedYear(),
                draft.getEmployeeCount(),
                draft.getAddress(),
                draft.getContactPage(),
                draft.getSourceUrl(),
                draft.getScrapedAt(),
                draft.getConfidenceScore(),
                draft.getProviderName(),
                draft.getNotes().isEmpty() ? null : String.join("; ", draft.getNotes()),
                List.copyOf(draft.getCategoryIds()),
                draft.getRawAttributes()
        );
    }

    private String buildCheckpoint(DiscoveredCompany company) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "phase", JobPhase.ENRICHMENT.name(),
                    "companyId", company.externalId() != null ? company.externalId() : company.name(),
                    "companyName", company.name()
            ));
        } catch (JsonProcessingException ex) {
            return company.externalId();
        }
    }
}
