package com.datascraper.orchestrator.service.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.IntelligenceJobStatus;
import com.datascraper.common.enums.ScraperExecutionStatus;
import com.datascraper.orchestrator.dto.IntelligenceJobRequest;
import com.datascraper.orchestrator.dto.IntelligenceJobResponse;
import com.datascraper.orchestrator.factory.ScraperFactory;
import com.datascraper.orchestrator.scraper.Scraper;
import com.datascraper.orchestrator.service.IntelligenceOrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs intelligence jobs by delegating to scraper strategies selected by the Factory.
 * Phase 11 upgrades this to parallel {@code CompletableFuture} execution.
 */
@Slf4j
@Service
public class IntelligenceOrchestratorServiceImpl implements IntelligenceOrchestratorService {

    private final ScraperFactory scraperFactory;

    public IntelligenceOrchestratorServiceImpl(ScraperFactory scraperFactory) {
        this.scraperFactory = scraperFactory;
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
        log.info("Job {} selected {} scraper(s): {}", jobId, scrapers.size(),
                scrapers.stream().map(scraper -> scraper.type().name()).toList());

        List<ScraperResult> results = new ArrayList<>();
        for (Scraper scraper : scrapers) {
            results.add(scraper.scrape(context));
        }

        IntelligenceJobStatus status = deriveStatus(results);
        long elapsed = System.currentTimeMillis() - start;

        return new IntelligenceJobResponse(
                jobId,
                status,
                "Executed %d scraper(s)".formatted(results.size()),
                elapsed,
                results
        );
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
