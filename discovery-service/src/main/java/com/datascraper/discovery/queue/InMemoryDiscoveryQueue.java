package com.datascraper.discovery.queue;

import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.common.dto.messaging.DiscoveryQueueMessage;
import com.datascraper.common.queue.PlatformQueues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class InMemoryDiscoveryQueue implements DiscoveryQueuePort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDiscoveryQueue.class);

    private final BlockingQueue<DiscoveryQueueMessage> discoveryQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<CompanyEnrichmentMessage> enrichmentQueue = new LinkedBlockingQueue<>();
    private final ObjectMapper objectMapper;

    public InMemoryDiscoveryQueue(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<DiscoveryQueueMessage> poll() {
        try {
            DiscoveryQueueMessage message = discoveryQueue.poll(1, TimeUnit.SECONDS);
            return Optional.ofNullable(message);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    public void offer(DiscoveryQueueMessage message) {
        discoveryQueue.offer(message);
    }

    @Override
    public void enqueueEnrichment(CompanyEnrichmentMessage message) {
        enrichmentQueue.offer(message);
        log.debug("Enqueued enrichment message for job {}", message.jobId());
    }

    public Optional<CompanyEnrichmentMessage> pollEnrichment() {
        return Optional.ofNullable(enrichmentQueue.poll());
    }

    public String serializeEnrichment(CompanyEnrichmentMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize enrichment message", ex);
        }
    }

    @Override
    public String discoveryQueueKey() {
        return PlatformQueues.DISCOVERY;
    }
}
