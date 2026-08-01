package com.datascraper.orchestrator.client;

import com.datascraper.common.dto.job.JobProgressUpdate;
import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;
import com.datascraper.orchestrator.config.OrchestratorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class JobServiceClient {

    private final WebClient webClient;
    private final OrchestratorProperties properties;

    public JobServiceClient(WebClient webClient, OrchestratorProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public JobResponse getJob(UUID jobId) {
        try {
            return webClient.get()
                    .uri(normalizeBaseUrl(properties.getJobServiceUri()) + "/api/jobs/{id}", jobId)
                    .retrieve()
                    .bodyToMono(JobResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.warn("Unable to fetch job {}: HTTP {}", jobId, ex.getStatusCode());
            return null;
        } catch (Exception ex) {
            log.warn("Unable to fetch job {}: {}", jobId, ex.getMessage());
            return null;
        }
    }

    public JobResponse patchProgress(JobProgressUpdate update) {
        try {
            return webClient.patch()
                    .uri(normalizeBaseUrl(properties.getJobServiceUri()) + "/api/jobs/{id}/progress", update.jobId())
                    .bodyValue(update)
                    .retrieve()
                    .bodyToMono(JobResponse.class)
                    .block();
        } catch (Exception ex) {
            log.warn("Unable to patch progress for job {}: {}", update.jobId(), ex.getMessage());
            return null;
        }
    }

    public JobProgressUpdate enrichmentProgress(
            UUID jobId,
            JobResponse current,
            int enrichedCount,
            int persistedCount,
            int failedCount,
            String message,
            String checkpoint
    ) {
        int discovered = current != null ? current.discoveredCount() : enrichedCount;
        int progress = discovered > 0 ? Math.min(99, (enrichedCount * 100) / discovered) : 25;
        return new JobProgressUpdate(
                jobId,
                JobStatus.RUNNING,
                JobPhase.ENRICHMENT,
                discovered,
                enrichedCount,
                persistedCount,
                failedCount,
                progress,
                null,
                message,
                checkpoint,
                Instant.now()
        );
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
