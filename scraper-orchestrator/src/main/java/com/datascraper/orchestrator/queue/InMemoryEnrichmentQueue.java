package com.datascraper.orchestrator.queue;

import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.common.queue.PlatformQueues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class InMemoryEnrichmentQueue implements EnrichmentQueuePort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEnrichmentQueue.class);

    private final BlockingQueue<CompanyEnrichmentMessage> queue = new LinkedBlockingQueue<>();

    @Override
    public Optional<CompanyEnrichmentMessage> poll() {
        try {
            CompanyEnrichmentMessage message = queue.poll(1, TimeUnit.SECONDS);
            return Optional.ofNullable(message);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public void offer(CompanyEnrichmentMessage message) {
        queue.offer(message);
        log.debug("Enqueued enrichment message for job {}", message.jobId());
    }

    @Override
    public void signalResume(UUID jobId) {
        log.debug("Resume signaled for job {} on in-memory queue", jobId);
    }

    @Override
    public String queueKey() {
        return PlatformQueues.COMPANY_ENRICHMENT;
    }
}
