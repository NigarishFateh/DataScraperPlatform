package com.datascraper.job.service;

import com.datascraper.common.dto.export.ExportRequest;
import com.datascraper.common.enums.ExportFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

/**
 * Best-effort trigger so failed/cancelled jobs still get a downloadable Excel of saved rows.
 */
@Service
public class ExportNotifyClient {

    private static final Logger log = LoggerFactory.getLogger(ExportNotifyClient.class);

    private final WebClient webClient;

    public ExportNotifyClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.export.base-url:http://localhost:8088}") String baseUrl
    ) {
        this.webClient = webClientBuilder.baseUrl(trimSlash(baseUrl)).build();
    }

    public void triggerPartialExport(UUID jobId, int persistedCount) {
        if (jobId == null || persistedCount <= 0) {
            return;
        }
        try {
            webClient.post()
                    .uri("/api/exports")
                    .bodyValue(new ExportRequest(jobId, ExportFormat.EXCEL))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Triggered partial Excel export for job {} ({} persisted rows)", jobId, persistedCount);
        } catch (Exception ex) {
            log.warn("Partial export trigger failed for job {}: {}", jobId, ex.getMessage());
        }
    }

    private static String trimSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8088";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
