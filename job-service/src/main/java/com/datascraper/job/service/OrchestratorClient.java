package com.datascraper.job.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

@Service
public class OrchestratorClient {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorClient.class);

    private final WebClient webClient;
    private final String resumePath;

    public OrchestratorClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.orchestrator.base-url}") String baseUrl,
            @Value("${app.orchestrator.resume-path}") String resumePath
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.resumePath = resumePath;
    }

    public void notifyResume(UUID jobId, String correlationId) {
        String path = resumePath.replace("{jobId}", jobId.toString());
        try {
            webClient.post()
                    .uri(path)
                    .header("X-Correlation-Id", correlationId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            log.warn("Orchestrator resume notification returned {} for job {}: {}",
                    ex.getStatusCode(), jobId, ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("Orchestrator resume notification failed for job {}: {}", jobId, ex.getMessage());
        }
    }
}
