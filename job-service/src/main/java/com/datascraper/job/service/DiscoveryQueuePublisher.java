package com.datascraper.job.service;

import com.datascraper.common.dto.messaging.DiscoveryQueueMessage;
import com.datascraper.common.queue.PlatformQueues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Publishes discovery work via Redis (when enabled) or async HTTP to discovery-service.
 */
@Service
public class DiscoveryQueuePublisher implements QueueService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryQueuePublisher.class);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectProvider<JobService> jobServiceProvider;
    private final InMemoryQueueService inMemoryQueueService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String discoveryServiceUri;
    private final boolean redisEnabled;
    private final ExecutorService dispatchExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "discovery-dispatch");
        t.setDaemon(true);
        return t;
    });

    public DiscoveryQueuePublisher(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectProvider<JobService> jobServiceProvider,
            InMemoryQueueService inMemoryQueueService,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder,
            @Value("${app.discovery.base-url:http://localhost:8087}") String discoveryServiceUri,
            @Value("${app.redis.enabled:false}") boolean redisEnabled
    ) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.jobServiceProvider = jobServiceProvider;
        this.inMemoryQueueService = inMemoryQueueService;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.build();
        this.discoveryServiceUri = trimTrailingSlash(discoveryServiceUri);
        this.redisEnabled = redisEnabled;
    }

    @Override
    public void publishDiscovery(DiscoveryQueueMessage message) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize discovery queue message", ex);
        }

        inMemoryQueueService.enqueue(message);

        boolean redisPublished = false;
        if (redisEnabled) {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForList().rightPush(PlatformQueues.DISCOVERY, payload);
                    redisPublished = true;
                    log.info("Published discovery job {} to Redis queue", message.jobId());
                } catch (Exception ex) {
                    log.warn("Redis discovery publish failed: {}", ex.getMessage());
                }
            }
        }

        if (!redisPublished) {
            dispatchExecutor.execute(() -> dispatchHttp(message));
        }
    }

    private void dispatchHttp(DiscoveryQueueMessage message) {
        try {
            log.info("Dispatching discovery job {} to discovery-service via HTTP", message.jobId());
            webClient.post()
                    .uri(discoveryServiceUri + "/api/discovery/consume")
                    .bodyValue(message)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofMinutes(30));
            log.info("Discovery HTTP dispatch completed for job {}", message.jobId());
        } catch (Exception ex) {
            log.error(
                    "Failed to dispatch discovery job {} via HTTP: {}",
                    message.jobId(),
                    ex.getMessage()
            );
            failJobBestEffort(
                    message.jobId(),
                    "Discovery service unreachable or failed: " + truncate(ex.getMessage(), 240)
            );
        }
    }

    private void failJobBestEffort(java.util.UUID jobId, String message) {
        try {
            JobService jobService = jobServiceProvider.getIfAvailable();
            if (jobService != null) {
                jobService.failJob(jobId, message == null ? "Discovery dispatch failed" : message);
                return;
            }
            log.warn("JobService unavailable; cannot mark job {} failed after discovery dispatch error", jobId);
        } catch (Exception failEx) {
            log.warn("Unable to mark job {} failed after discovery dispatch error: {}", jobId, failEx.getMessage());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    @Override
    public Optional<DiscoveryQueueMessage> pollDiscovery() {
        return inMemoryQueueService.poll();
    }

    @Override
    public int discoveryQueueSize() {
        return inMemoryQueueService.size();
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
