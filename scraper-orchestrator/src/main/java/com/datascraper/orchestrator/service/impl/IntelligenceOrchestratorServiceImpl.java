package com.datascraper.orchestrator.service.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.IntelligenceJobStatus;
import com.datascraper.common.enums.ScraperExecutionStatus;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import com.datascraper.orchestrator.dto.IntelligenceJobRequest;
import com.datascraper.orchestrator.dto.IntelligenceJobResponse;
import com.datascraper.orchestrator.factory.ScraperFactory;
import com.datascraper.orchestrator.scraper.Scraper;
import com.datascraper.orchestrator.service.IntelligenceOrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs intelligence jobs by delegating to scraper strategies in parallel via {@link CompletableFuture}.
 */
@Slf4j
@Service
public class IntelligenceOrchestratorServiceImpl implements IntelligenceOrchestratorService {

    private final ScraperFactory scraperFactory;
    private final Executor scraperExecutor;
    private final IntelligenceScraperProperties properties;

    public IntelligenceOrchestratorServiceImpl(
            ScraperFactory scraperFactory,
            @Qualifier("scraperExecutor") Executor scraperExecutor,
            IntelligenceScraperProperties properties
    ) {
        this.scraperFactory = scraperFactory;
        this.scraperExecutor = scraperExecutor;
        this.properties = properties;
    }

    @Override
    public IntelligenceJobResponse runJob(IntelligenceJobRequest request, String correlationId) {
        long start = System.currentTimeMillis();
        String jobId = UUID.randomUUID().toString();

        ScraperContext context = new ScraperContext(
                jobId,
                request.companyId(),
                request.companyName(),
                request.websiteUrl(),
                request.categoryIds() != null ? request.categoryIds() : List.of(),
                correlationId
        );

        List<Scraper> scrapers = scraperFactory.resolve(context, request.scraperTypes());
        log.info("Job {} selected {} scraper(s) for parallel execution: {}", jobId, scrapers.size(),
                scrapers.stream().map(scraper -> scraper.type().name()).toList());

        List<ScraperResult> results = executeInParallel(jobId, context, scrapers);
        IntelligenceJobStatus status = deriveStatus(results);
        long elapsed = System.currentTimeMillis() - start;

        return new IntelligenceJobResponse(
                jobId,
                status,
                "Executed %d scraper(s) in parallel".formatted(results.size()),
                elapsed,
                results
        );
    }

    private List<ScraperResult> executeInParallel(String jobId, ScraperContext context, List<Scraper> scrapers) {
        if (scrapers.isEmpty()) {
            return List.of();
        }

        List<CompletableFuture<ScraperResult>> futures = scrapers.stream()
                .map(scraper -> CompletableFuture.supplyAsync(
                        () -> runScraperSafely(scraper, context),
                        scraperExecutor
                ))
                .toList();

        long jobTimeoutMs = properties.getExecution().getJobTimeoutMs();
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .orTimeout(jobTimeoutMs, TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof TimeoutException) {
                log.warn("Job {} timed out after {} ms — returning partial results", jobId, jobTimeoutMs);
            } else {
                throw ex;
            }
        }

        List<ScraperResult> results = new ArrayList<>(futures.size());
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<ScraperResult> future = futures.get(i);
            Scraper scraper = scrapers.get(i);
            if (future.isDone() && !future.isCompletedExceptionally()) {
                results.add(future.join());
            } else {
                future.cancel(true);
                results.add(ScraperResult.failed(
                        scraper.type(),
                        "Scraper timed out after %d ms".formatted(jobTimeoutMs)
                ));
            }
        }
        return results;
    }

    private ScraperResult runScraperSafely(Scraper scraper, ScraperContext context) {
        ScraperType type = scraper.type();
        try {
            log.debug("Starting {} scraper for job {}", type, context.jobId());
            return scraper.scrape(context);
        } catch (Exception ex) {
            log.error("Unexpected error from {} scraper for job {}", type, context.jobId(), ex);
            return ScraperResult.failed(type, ex.getMessage() != null ? ex.getMessage() : "Unexpected scraper error");
        }
    }

    private IntelligenceJobStatus deriveStatus(List<ScraperResult> results) {
        if (results.isEmpty()) {
            return IntelligenceJobStatus.FAILED;
        }
        long failed = results.stream()
                .filter(result -> result.status() == ScraperExecutionStatus.FAILED)
                .count();
        long success = results.stream()
                .filter(result -> result.status() == ScraperExecutionStatus.SUCCESS)
                .count();

        if (failed == 0) {
            return IntelligenceJobStatus.COMPLETED;
        }
        if (success > 0) {
            return IntelligenceJobStatus.PARTIAL;
        }
        return IntelligenceJobStatus.FAILED;
    }
}
