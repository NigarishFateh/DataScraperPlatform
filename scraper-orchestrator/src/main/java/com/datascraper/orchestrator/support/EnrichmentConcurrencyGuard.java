package com.datascraper.orchestrator.support;

import com.datascraper.orchestrator.config.OrchestratorProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Caps concurrent HTTP enrichments so a misconfigured client cannot exhaust Tomcat threads.
 * Limit matches queue consumer concurrency (clamped 1–16) with a small headroom multiplier.
 */
@Component
public class EnrichmentConcurrencyGuard {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentConcurrencyGuard.class);
    private static final int MIN = 1;
    private static final int MAX = 32;
    private static final long ACQUIRE_TIMEOUT_SECONDS = 90;

    private final Semaphore permits;
    private final int maxConcurrent;

    public EnrichmentConcurrencyGuard(OrchestratorProperties properties) {
        int configured = properties.getQueue().getConsumerConcurrency();
        // HTTP path + queue path may overlap; allow 2× consumer concurrency, hard-capped.
        this.maxConcurrent = clamp(Math.max(configured * 2, configured + 4), MIN, MAX);
        this.permits = new Semaphore(this.maxConcurrent, true);
        log.info("Enrichment concurrency guard ready: maxConcurrent={}", this.maxConcurrent);
    }

    public <T> T run(Supplier<T> work) {
        boolean acquired;
        try {
            acquired = permits.tryAcquire(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Enrichment interrupted");
        }
        if (!acquired) {
            log.warn("Enrichment rejected: all {} slots busy for {}s", maxConcurrent, ACQUIRE_TIMEOUT_SECONDS);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Enrichment capacity busy; retry shortly"
            );
        }
        try {
            return work.get();
        } finally {
            permits.release();
        }
    }

    @PreDestroy
    public void shutdown() {
        // Nothing to close; permits drain as in-flight work finishes.
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
