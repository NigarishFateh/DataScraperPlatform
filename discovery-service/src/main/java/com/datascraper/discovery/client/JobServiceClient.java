package com.datascraper.discovery.client;

import com.datascraper.common.dto.job.JobProgressUpdate;
import com.datascraper.discovery.config.AppProperties;
import com.datascraper.discovery.dto.JobFailRequest;
import com.datascraper.discovery.dto.JobProgressPatchRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.UUID;

@Component
public class JobServiceClient {

    private final WebClient webClient;

    public JobServiceClient(WebClient.Builder webClientBuilder, AppProperties appProperties) {
        this.webClient = webClientBuilder
                .baseUrl(trimTrailingSlash(appProperties.getJobServiceUri()))
                .build();
    }

    public void patchProgress(UUID jobId, JobProgressPatchRequest request) {
        JobProgressUpdate body = new JobProgressUpdate(
                jobId,
                request.status(),
                request.phase(),
                request.discoveredCount() == null ? 0 : request.discoveredCount(),
                request.enrichedCount() == null ? 0 : request.enrichedCount(),
                request.persistedCount() == null ? 0 : request.persistedCount(),
                request.failedCount() == null ? 0 : request.failedCount(),
                request.progressPercent() == null ? 0 : request.progressPercent(),
                null,
                request.message(),
                request.checkpoint(),
                Instant.now()
        );
        webClient.patch()
                .uri("/api/jobs/{id}/progress", jobId)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void failJob(UUID jobId, String message) {
        webClient.post()
                .uri("/api/jobs/{id}/fail", jobId)
                .bodyValue(new JobFailRequest(message))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8086";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
