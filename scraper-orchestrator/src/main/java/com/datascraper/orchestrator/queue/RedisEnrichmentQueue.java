package com.datascraper.orchestrator.queue;

import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.common.queue.PlatformQueues;
import com.datascraper.orchestrator.config.OrchestratorProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RedisEnrichmentQueue implements EnrichmentQueuePort {

    private static final Logger log = LoggerFactory.getLogger(RedisEnrichmentQueue.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration blockTimeout;
    private final Set<UUID> pausedJobs = ConcurrentHashMap.newKeySet();

    public RedisEnrichmentQueue(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            OrchestratorProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.blockTimeout = Duration.ofSeconds(properties.getQueue().getBlockTimeoutSeconds());
    }

    @Override
    public Optional<CompanyEnrichmentMessage> poll() {
        try {
            String payload = redisTemplate.opsForList()
                    .rightPop(PlatformQueues.COMPANY_ENRICHMENT, blockTimeout);
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            CompanyEnrichmentMessage message = objectMapper.readValue(payload, CompanyEnrichmentMessage.class);
            if (pausedJobs.contains(message.jobId())) {
                redisTemplate.opsForList().leftPush(PlatformQueues.COMPANY_ENRICHMENT, payload);
                return Optional.empty();
            }
            return Optional.of(message);
        } catch (Exception ex) {
            log.warn("Failed to poll enrichment queue from Redis: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void offer(CompanyEnrichmentMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush(PlatformQueues.COMPANY_ENRICHMENT, payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize enrichment message", ex);
        }
    }

    @Override
    public void signalResume(UUID jobId) {
        pausedJobs.remove(jobId);
    }

    @Override
    public String queueKey() {
        return PlatformQueues.COMPANY_ENRICHMENT;
    }

    public void markPaused(UUID jobId) {
        pausedJobs.add(jobId);
    }
}
