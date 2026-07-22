package com.datascraper.orchestrator.service;

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
import com.datascraper.orchestrator.service.impl.IntelligenceOrchestratorServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntelligenceOrchestratorServiceImplTest {

    @Mock
    private ScraperFactory scraperFactory;

    private Executor scraperExecutor;
    private IntelligenceOrchestratorService service;

    @BeforeEach
    void setUp() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("test-scraper-");
        executor.initialize();
        scraperExecutor = executor;

        IntelligenceScraperProperties properties = new IntelligenceScraperProperties();
        properties.getExecution().setJobTimeoutMs(5000);

        service = new IntelligenceOrchestratorServiceImpl(scraperFactory, scraperExecutor, properties);
    }

    @AfterEach
    void tearDown() {
        if (scraperExecutor instanceof ThreadPoolTaskExecutor taskExecutor) {
            taskExecutor.shutdown();
        }
    }

    @Test
    void runsScrapersInParallel() {
        int delayMs = 200;
        List<Scraper> scrapers = List.of(
                delayedScraper(ScraperType.COMPANY_WEBSITE, delayMs),
                delayedScraper(ScraperType.TECHNOLOGY_STACK, delayMs),
                delayedScraper(ScraperType.NEWS, delayMs)
        );
        when(scraperFactory.resolve(any(ScraperContext.class), eq(null))).thenReturn(scrapers);

        IntelligenceJobRequest request = new IntelligenceJobRequest(
                "co-1", "Acme", "https://acme.example", List.of("cloud"), null);

        long start = System.currentTimeMillis();
        IntelligenceJobResponse response = service.runJob(request, "corr-1");
        long elapsed = System.currentTimeMillis() - start;

        assertThat(response.status()).isEqualTo(IntelligenceJobStatus.COMPLETED);
        assertThat(response.results()).hasSize(3);
        assertThat(response.results())
                .extracting(ScraperResult::status)
                .containsOnly(ScraperExecutionStatus.SUCCESS);
        assertThat(elapsed).isLessThan(delayMs * 3L);
    }

    @Test
    void returnsPartialWhenOneScraperFails() {
        Scraper ok = delayedScraper(ScraperType.COMPANY_WEBSITE, 10);
        Scraper fail = scraper(ScraperType.TECHNOLOGY_STACK, () ->
                ScraperResult.failed(ScraperType.TECHNOLOGY_STACK, "down"));
        when(scraperFactory.resolve(any(ScraperContext.class), eq(null))).thenReturn(List.of(ok, fail));

        IntelligenceJobRequest request = new IntelligenceJobRequest(
                "co-1", "Acme", "https://acme.example", List.of(), null);

        IntelligenceJobResponse response = service.runJob(request, "corr-1");

        assertThat(response.status()).isEqualTo(IntelligenceJobStatus.PARTIAL);
        assertThat(response.results()).hasSize(2);
    }

    @Test
    void marksTimedOutScrapersAsFailed() {
        IntelligenceScraperProperties properties = new IntelligenceScraperProperties();
        properties.getExecution().setJobTimeoutMs(50);
        service = new IntelligenceOrchestratorServiceImpl(scraperFactory, scraperExecutor, properties);

        when(scraperFactory.resolve(any(ScraperContext.class), eq(null)))
                .thenReturn(List.of(delayedScraper(ScraperType.COMPANY_WEBSITE, 500)));

        IntelligenceJobRequest request = new IntelligenceJobRequest(
                "co-1", "Acme", "https://acme.example", List.of(), null);

        IntelligenceJobResponse response = service.runJob(request, "corr-1");

        assertThat(response.status()).isEqualTo(IntelligenceJobStatus.FAILED);
        assertThat(response.results()).singleElement()
                .satisfies(result -> {
                    assertThat(result.status()).isEqualTo(ScraperExecutionStatus.FAILED);
                    assertThat(result.message()).contains("timed out");
                });
    }

    private static Scraper delayedScraper(ScraperType type, int delayMs) {
        AtomicInteger calls = new AtomicInteger();
        return scraper(type, () -> {
            calls.incrementAndGet();
            sleep(delayMs);
            return ScraperResult.success(type, "ok", List.of(), java.util.Map.of());
        });
    }

    private static Scraper scraper(ScraperType type, java.util.concurrent.Callable<ScraperResult> action) {
        return new Scraper() {
            @Override
            public ScraperType type() {
                return type;
            }

            @Override
            public boolean supports(ScraperContext context) {
                return true;
            }

            @Override
            public ScraperResult scrape(ScraperContext context) {
                try {
                    return action.call();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
