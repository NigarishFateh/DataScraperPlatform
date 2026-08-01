package com.datascraper.orchestrator.service;

import com.datascraper.common.dto.provider.ProviderContext;
import com.datascraper.common.dto.provider.ProviderResult;
import com.datascraper.common.provider.CompanyDataProvider;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import com.datascraper.orchestrator.factory.CompanyDataProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class EnrichmentService {

    private final CompanyDataProviderFactory providerFactory;
    private final Executor scraperExecutor;
    private final IntelligenceScraperProperties properties;

    public EnrichmentService(
            CompanyDataProviderFactory providerFactory,
            @Qualifier("scraperExecutor") Executor scraperExecutor,
            IntelligenceScraperProperties properties
    ) {
        this.providerFactory = providerFactory;
        this.scraperExecutor = scraperExecutor;
        this.properties = properties;
    }

    public List<ProviderResult> enrich(ProviderContext context, List<String> requestedProviders) {
        List<CompanyDataProvider> providers = providerFactory.resolve(context, requestedProviders);
        if (providers.isEmpty()) {
            log.warn("No enrichment providers selected for job {} company {}", context.jobId(), context.companyId());
            return List.of();
        }

        log.info("Enriching job {} company {} with {} provider(s)",
                context.jobId(), context.companyId(), providers.size());

        List<CompletableFuture<ProviderResult>> futures = providers.stream()
                .map(provider -> CompletableFuture.supplyAsync(
                        () -> runProviderSafely(provider, context),
                        scraperExecutor
                ))
                .toList();

        long timeoutMs = properties.getExecution().getJobTimeoutMs();
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException ex) {
            if (!(ex.getCause() instanceof TimeoutException)) {
                throw ex;
            }
            log.warn("Enrichment timed out after {} ms for job {}", timeoutMs, context.jobId());
        }

        List<ProviderResult> results = new ArrayList<>(futures.size());
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<ProviderResult> future = futures.get(i);
            CompanyDataProvider provider = providers.get(i);
            if (future.isDone() && !future.isCompletedExceptionally()) {
                results.add(future.join());
            } else {
                future.cancel(true);
                results.add(ProviderResult.failed(
                        provider.type(),
                        provider.name(),
                        "Provider timed out after %d ms".formatted(timeoutMs)
                ));
            }
        }
        return results;
    }

    private ProviderResult runProviderSafely(CompanyDataProvider provider, ProviderContext context) {
        try {
            return provider.enrich(context);
        } catch (Exception ex) {
            log.error("Provider {} failed unexpectedly for job {}", provider.type(), context.jobId(), ex);
            return ProviderResult.failed(
                    provider.type(),
                    provider.name(),
                    ex.getMessage() != null ? ex.getMessage() : "Unexpected provider error"
            );
        }
    }
}
