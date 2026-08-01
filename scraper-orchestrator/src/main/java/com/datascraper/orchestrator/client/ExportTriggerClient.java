package com.datascraper.orchestrator.client;

import com.datascraper.common.dto.export.ExportRequest;
import com.datascraper.common.enums.ExportFormat;
import com.datascraper.common.queue.PlatformQueues;
import com.datascraper.orchestrator.config.OrchestratorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class ExportTriggerClient {

    private final WebClient webClient;
    private final OrchestratorProperties properties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public ExportTriggerClient(
            WebClient webClient,
            OrchestratorProperties properties,
            ObjectMapper objectMapper,
            @Autowired(required = false) StringRedisTemplate redisTemplate
    ) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public void triggerExport(UUID jobId) {
        publishToQueue(jobId);
        postExportRequest(jobId);
    }

    private void publishToQueue(UUID jobId) {
        if (!properties.getRedis().isEnabled() || redisTemplate == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of("jobId", jobId.toString()));
            redisTemplate.opsForList().leftPush(PlatformQueues.EXPORT, payload);
            log.info("Published export queue message for job {}", jobId);
        } catch (Exception ex) {
            log.warn("Failed to publish export queue message for job {}: {}", jobId, ex.getMessage());
        }
    }

    private void postExportRequest(UUID jobId) {
        try {
            webClient.post()
                    .uri(normalizeBaseUrl(properties.getExportServiceUri()) + "/api/exports")
                    .bodyValue(new ExportRequest(jobId, ExportFormat.EXCEL))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Triggered export HTTP request for job {}", jobId);
        } catch (Exception ex) {
            log.warn("Export HTTP trigger failed for job {}: {}", jobId, ex.getMessage());
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
