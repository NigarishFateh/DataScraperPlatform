package com.datascraper.discovery.queue;

import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.common.dto.messaging.DiscoveryQueueMessage;
import com.datascraper.common.queue.PlatformQueues;
import com.datascraper.discovery.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

public class RedisDiscoveryQueue implements DiscoveryQueuePort {

    private static final Logger log = LoggerFactory.getLogger(RedisDiscoveryQueue.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration blockTimeout;

    public RedisDiscoveryQueue(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AppProperties appProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.blockTimeout = Duration.ofSeconds(appProperties.getQueue().getBlockTimeoutSeconds());
    }

    @Override
    public Optional<DiscoveryQueueMessage> poll() {
        try {
            String payload = redisTemplate.opsForList()
                    .rightPop(PlatformQueues.DISCOVERY, blockTimeout);
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, DiscoveryQueueMessage.class));
        } catch (Exception ex) {
            log.warn("Failed to poll discovery queue from Redis: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void enqueueEnrichment(CompanyEnrichmentMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush(PlatformQueues.COMPANY_ENRICHMENT, payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize enrichment message", ex);
        }
    }

    @Override
    public String discoveryQueueKey() {
        return PlatformQueues.DISCOVERY;
    }
}
