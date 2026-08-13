package com.datascraper.discovery.service;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.common.dto.messaging.DiscoveryQueueMessage;
import com.datascraper.common.enums.ProviderExecutionStatus;
import com.datascraper.common.provider.DiscoveryProvider;
import com.datascraper.discovery.client.JobServiceClient;
import com.datascraper.discovery.client.OrchestratorEnrichmentClient;
import com.datascraper.discovery.config.AppProperties;
import com.datascraper.discovery.dto.DiscoveryRunResponse;
import com.datascraper.discovery.dto.JobProgressPatchRequest;
import com.datascraper.discovery.factory.DiscoveryProviderFactory;
import com.datascraper.discovery.provider.OpenDataDiscoveryProvider;
import com.datascraper.discovery.queue.DiscoveryQueuePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DiscoveryOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryOrchestrationService.class);

    private final DiscoveryProviderFactory providerFactory;
    private final CompanyDeduplicationService deduplicationService;
    private final DiscoveryLogService discoveryLogService;
    private final DiscoveryQueuePort discoveryQueuePort;
    private final JobServiceClient jobServiceClient;
    private final OrchestratorEnrichmentClient orchestratorEnrichmentClient;
    private final AppProperties appProperties;

    public DiscoveryOrchestrationService(
            DiscoveryProviderFactory providerFactory,
            CompanyDeduplicationService deduplicationService,
            DiscoveryLogService discoveryLogService,
            DiscoveryQueuePort discoveryQueuePort,
            JobServiceClient jobServiceClient,
            OrchestratorEnrichmentClient orchestratorEnrichmentClient,
            AppProperties appProperties
    ) {
        this.providerFactory = providerFactory;
        this.deduplicationService = deduplicationService;
        this.discoveryLogService = discoveryLogService;
        this.discoveryQueuePort = discoveryQueuePort;
        this.jobServiceClient = jobServiceClient;
        this.orchestratorEnrichmentClient = orchestratorEnrichmentClient;
        this.appProperties = appProperties;
    }

    public DiscoveryRunResponse runSync(DiscoveryRequest request, List<String> enabledProviders) {
        UUID jobId = parseJobId(request.jobId());
        return executeDiscovery(jobId, request, enabledProviders, true);
    }

    public DiscoveryRunResponse processQueueMessage(DiscoveryQueueMessage message) {
        DiscoveryRequest request = toDiscoveryRequest(message);
        return executeDiscovery(message.jobId(), request, message.enabledProviders(), true);
    }

    private DiscoveryRunResponse executeDiscovery(
            UUID jobId,
            DiscoveryRequest request,
            List<String> enabledProviders,
            boolean notifyJobService
    ) {
        if (notifyJobService) {
            notifyProgress(jobId, "Starting discovery phase");
        }

        List<DiscoveryProvider> providers = providerFactory.getEnabledProviders(enabledProviders);
        List<DiscoveredCompany> aggregated = new ArrayList<>();
        List<DiscoveryLogService.ProviderExecutionRecord> executionRecords = new ArrayList<>();

        for (DiscoveryProvider provider : providers) {
            String summary = buildRequestSummary(request, provider);
            try {
                List<DiscoveredCompany> found = provider.discover(request);
                aggregated.addAll(found);

                if (provider instanceof OpenDataDiscoveryProvider openData && found.isEmpty()) {
                    List<String> attempts = openData.attemptSummaries(request);
                    summary = summary + "; attempts=" + String.join(",", attempts);
                }

                executionRecords.add(new DiscoveryLogService.ProviderExecutionRecord(
                        provider,
                        summary,
                        found.size(),
                        found.isEmpty() ? ProviderExecutionStatus.SKIPPED : ProviderExecutionStatus.SUCCESS,
                        found.isEmpty() ? "No catalog matches for provider heuristics" : "Discovery succeeded"
                ));

                if (notifyJobService && !aggregated.isEmpty()) {
                    int runningUnique = deduplicationService.deduplicate(aggregated).size();
                    notifyDiscoveredProgress(jobId, runningUnique);
                }
            } catch (Exception ex) {
                log.error("Provider {} failed for job {}: {}", provider.name(), jobId, ex.getMessage(), ex);
                executionRecords.add(new DiscoveryLogService.ProviderExecutionRecord(
                        provider,
                        summary,
                        0,
                        ProviderExecutionStatus.FAILED,
                        ex.getMessage()
                ));
            }
        }

        List<DiscoveredCompany> unique = deduplicationService.deduplicate(aggregated);
        int enqueued = enqueueEnrichmentMessages(jobId, request.correlationId(), unique, enabledProviders);

        discoveryLogService.saveLogs(jobId, executionRecords);

        if (notifyJobService) {
            try {
                if (unique.isEmpty()) {
                    boolean named = request.companyNames() != null && !request.companyNames().isEmpty();
                    if (named) {
                        log.warn("Named scrape produced 0 companies for job {} — failing with partial-export message", jobId);
                    }
                    jobServiceClient.failJob(jobId, buildEmptyDiscoveryMessage(request));
                } else {
                    jobServiceClient.patchProgress(jobId, JobProgressPatchRequest.discoveredCount(unique.size()));
                }
            } catch (Exception ex) {
                log.warn("Unable to update job {} after discovery: {}", jobId, ex.getMessage());
            }
        }

        return new DiscoveryRunResponse(
                jobId.toString(),
                unique.size(),
                enqueued,
                unique
        );
    }

    private int enqueueEnrichmentMessages(
            UUID jobId,
            String correlationId,
            List<DiscoveredCompany> companies,
            List<String> enabledProviders
    ) {
        int count = 0;
        for (DiscoveredCompany company : companies) {
            CompanyEnrichmentMessage message = new CompanyEnrichmentMessage(
                    jobId,
                    correlationId,
                    company,
                    enabledProviders == null ? List.of() : enabledProviders
            );
            boolean queued = false;
            try {
                discoveryQueuePort.enqueueEnrichment(message);
                queued = true;
            } catch (Exception ex) {
                log.warn("Queue enrichment publish failed for {}: {}", company.name(), ex.getMessage());
            }
            if (!queued || !appProperties.getRedis().isEnabled()) {
                orchestratorEnrichmentClient.enrichAsync(message);
            }
            count++;
        }
        return count;
    }

    private void notifyProgress(UUID jobId, String message) {
        try {
            jobServiceClient.patchProgress(jobId, JobProgressPatchRequest.discoveryPhase(message));
        } catch (Exception ex) {
            log.warn("Unable to notify job-service for job {}: {}", jobId, ex.getMessage());
        }
    }

    private void notifyDiscoveredProgress(UUID jobId, int count) {
        try {
            jobServiceClient.patchProgress(jobId, JobProgressPatchRequest.discoveredProgress(count));
        } catch (Exception ex) {
            log.warn("Unable to patch discovered count for job {}: {}", jobId, ex.getMessage());
        }
    }

    private DiscoveryRequest toDiscoveryRequest(DiscoveryQueueMessage message) {
        return new DiscoveryRequest(
                message.jobId().toString(),
                message.correlationId(),
                message.countryCodes(),
                message.cityIds(),
                message.categoryIds(),
                message.maxCompanies(),
                message.companyNames()
        );
    }

    private UUID parseJobId(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return UUID.randomUUID();
        }
        return UUID.fromString(jobId);
    }

    private String buildRequestSummary(DiscoveryRequest request, DiscoveryProvider provider) {
        return "provider=" + provider.type()
                + "; categories=" + join(request.categoryIds())
                + "; countries=" + join(request.countryCodes())
                + "; cities=" + join(request.cityIds())
                + "; maxResults=" + request.maxResults();
    }

    private String buildEmptyDiscoveryMessage(DiscoveryRequest request) {
        String categories = join(request.categoryIds());
        String countries = join(request.countryCodes());
        String cities = join(request.cityIds());
        boolean places = hasKey(appProperties.getGooglePlacesApiKey());
        boolean apollo = hasKey(appProperties.getApolloApiKey());
        boolean serp = hasKey(appProperties.getSerpapiApiKey());

        StringBuilder message = new StringBuilder();
        message.append("No companies found for categories=[").append(categories)
                .append("] countries=[").append(countries)
                .append("] cities=[").append(cities).append("].");
        if (request.cityIds() != null && !request.cityIds().isEmpty()) {
            message.append(" That city may be too small for this category — leave city empty to search the whole country, or pick a larger city.");
        }
        if (places) {
            message.append(" Google Places ran but returned no matches.");
        } else {
            message.append(" Set GOOGLE_PLACES_API_KEY in .env.");
        }
        if (apollo) {
            message.append(" Apollo may be out of lead credits (HTTP 422).");
        }
        if (serp) {
            message.append(" SerpAPI may be out of quota (HTTP 429).");
        }
        return message.toString();
    }

    private static boolean hasKey(String value) {
        return value != null && !value.isBlank();
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        return values.stream().collect(Collectors.joining(","));
    }
}
