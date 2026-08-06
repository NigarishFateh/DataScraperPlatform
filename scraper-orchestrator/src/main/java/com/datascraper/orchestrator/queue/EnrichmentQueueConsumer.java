package com.datascraper.orchestrator.queue;

import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.orchestrator.config.OrchestratorProperties;
import com.datascraper.orchestrator.service.CompanyEnrichmentPipelineService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(prefix = "app.queue", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EnrichmentQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentQueueConsumer.class);
    private static final int MIN_CONCURRENCY = 1;
    private static final int MAX_CONCURRENCY = 16;

    private final EnrichmentQueuePort enrichmentQueuePort;
    private final CompanyEnrichmentPipelineService pipelineService;
    private final Semaphore inFlight;
    private final ExecutorService workers;
    private final int concurrency;

    public EnrichmentQueueConsumer(
            EnrichmentQueuePort enrichmentQueuePort,
            CompanyEnrichmentPipelineService pipelineService,
            OrchestratorProperties properties
    ) {
        this.enrichmentQueuePort = enrichmentQueuePort;
        this.pipelineService = pipelineService;
        this.concurrency = clamp(properties.getQueue().getConsumerConcurrency(), MIN_CONCURRENCY, MAX_CONCURRENCY);
        this.inFlight = new Semaphore(this.concurrency);
        AtomicInteger threadIndex = new AtomicInteger(1);
        this.workers = Executors.newFixedThreadPool(this.concurrency, runnable -> {
            Thread t = new Thread(runnable, "enrichment-worker-" + threadIndex.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
        log.info("Enrichment queue consumer ready: concurrency={}", this.concurrency);
    }

    /**
     * Fills free worker slots from the queue. Returns quickly so the scheduler can re-poll;
     * work runs asynchronously up to {@code consumer-concurrency}.
     */
    @Scheduled(fixedDelayString = "${app.queue.poll-interval-ms:250}")
    public void pollEnrichmentQueue() {
        while (inFlight.tryAcquire()) {
            Optional<CompanyEnrichmentMessage> polled = enrichmentQueuePort.poll();
            if (polled.isEmpty()) {
                inFlight.release();
                return;
            }
            CompanyEnrichmentMessage message = polled.get();
            workers.execute(() -> processSafely(message));
        }
    }

    private void processSafely(CompanyEnrichmentMessage message) {
        try {
            log.info(
                    "Processing enrichment message for job {} company {}",
                    message.jobId(),
                    message.company().name()
            );
            pipelineService.process(message);
        } catch (Exception ex) {
            log.error(
                    "Failed enrichment for job {} company {}: {}",
                    message.jobId(),
                    message.company() == null ? "?" : message.company().name(),
                    ex.getMessage(),
                    ex
            );
        } finally {
            inFlight.release();
        }
    }

    @PreDestroy
    public void shutdown() {
        workers.shutdown();
        try {
            if (!workers.awaitTermination(60, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException ex) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
