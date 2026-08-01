package com.datascraper.export.queue;

import com.datascraper.common.dto.export.ExportRequest;
import com.datascraper.common.enums.ExportFormat;
import com.datascraper.common.queue.PlatformQueues;
import com.datascraper.export.config.ExportProperties;
import com.datascraper.export.service.ExportService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "export.queue", name = "enabled", havingValue = "true")
public class ExportQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExportQueueConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final ExportService exportService;
    private final ObjectMapper objectMapper;
    private final long pollIntervalMs;

    public ExportQueueConsumer(
            StringRedisTemplate redisTemplate,
            ExportService exportService,
            ObjectMapper objectMapper,
            ExportProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.exportService = exportService;
        this.objectMapper = objectMapper;
        this.pollIntervalMs = properties.getQueue().getPollIntervalMs();
    }

    @Scheduled(fixedDelayString = "${export.queue.poll-interval-ms:2000}")
    public void pollExportQueue() {
        try {
            String payload = redisTemplate.opsForList().rightPop(PlatformQueues.EXPORT, pollIntervalMs, TimeUnit.MILLISECONDS);
            if (payload == null || payload.isBlank()) {
                return;
            }
            UUID jobId = parseJobId(payload);
            if (jobId == null) {
                log.warn("Skipping export queue message with missing jobId: {}", payload);
                return;
            }
            log.info("Processing export queue message for job {}", jobId);
            exportService.createExport(new ExportRequest(jobId, ExportFormat.EXCEL));
        } catch (Exception ex) {
            log.error("Failed to process export queue message", ex);
        }
    }

    private UUID parseJobId(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.hasNonNull("jobId")) {
                return UUID.fromString(node.get("jobId").asText());
            }
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return UUID.fromString(payload.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
