package com.datascraper.discovery.client;

import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.discovery.config.AppProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cross-process enrichment dispatch so discovery works without a shared Redis instance.
 * Uses a bounded thread pool so large jobs apply backpressure instead of exhausting memory.
 */
@Component
public class OrchestratorEnrichmentClient {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorEnrichmentClient.class);
    private static final int MIN_CONCURRENCY = 1;
    private static final int MAX_CONCURRENCY = 16;
    private static final int FAIL_JOB_AFTER_CONSECUTIVE = 3;

    private final WebClient webClient;
    private final JobServiceClient jobServiceClient;
    private final String orchestratorBaseUrl;
    private final ThreadPoolExecutor executor;
    private final ConcurrentHashMap<UUID, AtomicInteger> consecutiveFailures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicBoolean> failedSignaled = new ConcurrentHashMap<>();

    public OrchestratorEnrichmentClient(
            WebClient.Builder webClientBuilder,
            AppProperties appProperties,
            JobServiceClient jobServiceClient
    ) {
        this.webClient = webClientBuilder.build();
        this.jobServiceClient = jobServiceClient;
        this.orchestratorBaseUrl = trimTrailingSlash(
                appProperties.getOrchestratorServiceUri() == null
                        ? "http://localhost:8085"
                        : appProperties.getOrchestratorServiceUri()
        );

        AppProperties.EnrichmentProperties enrichment = appProperties.getEnrichment();
        int concurrency = clamp(enrichment.getDispatchConcurrency(), MIN_CONCURRENCY, MAX_CONCURRENCY);
        int queueCapacity = clamp(
                enrichment.getDispatchQueueCapacity(),
                concurrency,
                200
        );
        AtomicInteger threadIndex = new AtomicInteger(1);
        this.executor = new ThreadPoolExecutor(
                concurrency,
                concurrency,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                runnable -> {
                    Thread t = new Thread(runnable, "enrichment-dispatch-" + threadIndex.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                },
                // Slow the discovery publisher instead of growing an unbounded queue / OOM.
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.executor.allowCoreThreadTimeOut(true);
        log.info(
                "Enrichment HTTP dispatch pool ready: concurrency={}, queueCapacity={}",
                concurrency,
                queueCapacity
        );
    }

    /**
     * Fire-and-forget enrichment so discovery can finish quickly.
     */
    public void enrichAsync(CompanyEnrichmentMessage message) {
        executor.execute(() -> enrich(message));
    }

    public void enrich(CompanyEnrichmentMessage message) {
        UUID jobId = message.jobId();
        try {
            webClient.post()
                    .uri(orchestratorBaseUrl + "/api/orchestrator/enrich")
                    .bodyValue(message)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofMinutes(5));
            if (jobId != null) {
                consecutiveFailures.computeIfAbsent(jobId, id -> new AtomicInteger()).set(0);
            }
        } catch (Exception ex) {
            log.warn(
                    "Failed to dispatch enrichment for job {} company {}: {}",
                    jobId,
                    message.company() == null ? "?" : message.company().name(),
                    ex.getMessage()
            );
            signalJobFailedIfStuck(jobId, ex.getMessage());
        }
    }

    private void signalJobFailedIfStuck(UUID jobId, String error) {
        if (jobId == null) {
            return;
        }
        int failures = consecutiveFailures
                .computeIfAbsent(jobId, id -> new AtomicInteger())
                .incrementAndGet();
        if (failures < FAIL_JOB_AFTER_CONSECUTIVE) {
            return;
        }
        AtomicBoolean signaled = failedSignaled.computeIfAbsent(jobId, id -> new AtomicBoolean(false));
        if (!signaled.compareAndSet(false, true)) {
            return;
        }
        try {
            jobServiceClient.failJob(
                    jobId,
                    "Enrichment orchestrator unreachable after " + failures
                            + " consecutive dispatch failures: " + truncate(error, 200)
            );
        } catch (Exception failEx) {
            log.warn("Unable to mark job {} failed after enrichment dispatch errors: {}", jobId, failEx.getMessage());
            signaled.set(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
