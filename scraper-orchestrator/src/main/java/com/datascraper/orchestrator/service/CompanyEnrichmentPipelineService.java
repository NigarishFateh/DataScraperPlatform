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

        String checkpoint = buildCheckpoint(message.company());
        try {
            ProviderContext context = ProviderContextMapper.fromMessage(message);
            List<String> providers = requestedProviders(message);
            List<ProviderResult> providerResults = enrichmentService.enrich(context, providers);

            CompanyDraft draft = aggregationService.aggregate(message.company(), providerResults);
            normalizationService.normalize(draft);
            ValidationOutcome validation = validationService.validate(draft);

            EnrichedCompany enrichedCompany = toEnrichedCompany(draft);
            boolean persisted = false;
            try {
                persisted = persistenceClient.persist(jobId, enrichedCompany);
            } catch (Exception persistEx) {
                log.warn("Persist failed for job {} company {}: {}", jobId, context.companyId(), persistEx.getMessage());
            }

            int enrichedCount = jobCompletionTracker.incrementEnriched(jobId);
            int persistedCount = persisted
                    ? jobCompletionTracker.incrementPersisted(jobId)
                    : jobCompletionTracker.currentPersistedCount(jobId);
            // Failed = not saved. Soft validation warnings must not double-count as failures.
            int failedCount = !persisted
                    ? jobCompletionTracker.incrementFailed(jobId)
                    : jobCompletionTracker.currentFailedCount(jobId);

            String progressMessage;
            if (persisted && validation.softFailure()) {
                progressMessage = "Persisted company " + context.companyId() + " with validation warnings";
            } else if (persisted) {
                progressMessage = "Persisted company " + context.companyId();
            } else {
                progressMessage = "Failed to persist company " + context.companyId();
            }

            JobResponse currentJob = jobServiceClient.getJob(jobId);
            jobServiceClient.patchProgress(jobServiceClient.enrichmentProgress(
                    jobId,
                    currentJob,
                    enrichedCount,
                    persistedCount,
                    failedCount,
                    progressMessage,
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
        } catch (Exception ex) {
            log.error("Enrichment failed for job {} company {}: {}",
                    jobId,
                    message.company() == null ? "?" : message.company().name(),
                    ex.getMessage(),
                    ex);

            int persistedCount = jobCompletionTracker.currentPersistedCount(jobId);
            try {
                CompanyDraft draft = aggregationService.aggregate(message.company(), List.of());
                normalizationService.normalize(draft);
                EnrichedCompany seedOnly = toEnrichedCompany(draft);
                if (persistenceClient.persist(jobId, seedOnly)) {
                    persistedCount = jobCompletionTracker.incrementPersisted(jobId);
                }
            } catch (Exception persistEx) {
                log.warn("Seed persist after enrichment error failed for job {}: {}", jobId, persistEx.getMessage());
            }

            int enrichedCount = jobCompletionTracker.incrementEnriched(jobId);
            int failedCount = jobCompletionTracker.incrementFailed(jobId);
            JobResponse currentJob = jobServiceClient.getJob(jobId);
            jobServiceClient.patchProgress(jobServiceClient.enrichmentProgress(
                    jobId,
                    currentJob,
                    enrichedCount,
                    persistedCount,
                    failedCount,
                    "Enrichment error: " + ex.getMessage(),
                    checkpoint
            ));
            jobCompletionTracker.checkAndTriggerExport(jobId, enrichedCount);

            return skippedResult(message, "Enrichment error: " + ex.getMessage());
        }
    }

    private List<String> requestedProviders(CompanyEnrichmentMessage message) {
        List<String> requested = message.enabledProviders();
        if (!hasPlacesPhoneAndAddress(message.company())) {
            return requested;
        }
        // Places already filled branch phone + address — skip the slow HTML contact crawl.
        if (requested == null || requested.isEmpty()) {
            return List.of("WEBSITE");
        }
        return requested.stream()
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> {
                    String upper = name.trim().toUpperCase();
                    return !upper.contains("CONTACT");
                })
                .toList();
    }

    private static boolean hasPlacesPhoneAndAddress(DiscoveredCompany company) {
        if (company == null || company.metadata() == null) {
            return false;
        }
        Object address = company.metadata().get("address");
        Object phone = company.metadata().get("phone");
        return address != null && !address.toString().isBlank()
                && phone != null && !phone.toString().isBlank();
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
