package com.datascraper.orchestrator.scraper;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperExecutionStatus;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.cache.ScraperResultCache;
import com.datascraper.orchestrator.client.ScraperServiceClient;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractRemoteScraperCacheTest {

    @Mock
    private ScraperServiceClient scraperServiceClient;

    @Mock
    private ScraperResultCache scraperResultCache;

    private IntelligenceScraperProperties properties;
    private Scraper scraper;
    private ScraperContext context;

    @BeforeEach
    void setUp() {
        properties = new IntelligenceScraperProperties();
        properties.getCache().setEnabled(true);
        properties.getResilience().setMaxRetries(1);

        IntelligenceScraperProperties.ServiceEndpoint endpoint = new IntelligenceScraperProperties.ServiceEndpoint();
        endpoint.setBaseUrl("http://localhost:8091");
        properties.getServices().put("website", endpoint);

        scraper = new AbstractRemoteScraper(scraperServiceClient, scraperResultCache, properties, "website") {
            @Override
            public ScraperType type() {
                return ScraperType.COMPANY_WEBSITE;
            }
        };

        context = new ScraperContext(
                "job-1", "co-1", "Acme", "https://acme.example", List.of(), "corr-1");
    }

    @Test
    void returnsCachedResultWithoutCallingRemoteService() {
        ScraperResult cached = ScraperResult.success(
                ScraperType.COMPANY_WEBSITE, "cached", List.of(), Map.of("fromCache", true));
        when(scraperResultCache.get(ScraperType.COMPANY_WEBSITE, context)).thenReturn(Optional.of(cached));

        ScraperResult result = scraper.scrape(context);

        assertThat(result.message()).isEqualTo("cached");
        verify(scraperServiceClient, never()).scrape(any(), any(), any());
    }

    @Test
    void cachesSuccessfulRemoteResult() {
        ScraperResult remote = ScraperResult.success(
                ScraperType.COMPANY_WEBSITE, "fresh", List.of(), Map.of());
        when(scraperResultCache.get(ScraperType.COMPANY_WEBSITE, context)).thenReturn(Optional.empty());
        when(scraperServiceClient.scrape(eq("http://localhost:8091"), eq(ScraperType.COMPANY_WEBSITE), eq(context)))
                .thenReturn(remote);

        ScraperResult result = scraper.scrape(context);

        assertThat(result.status()).isEqualTo(ScraperExecutionStatus.SUCCESS);
        verify(scraperResultCache).put(ScraperType.COMPANY_WEBSITE, context, remote);
    }

    @Test
    void doesNotCacheFailedRemoteResult() {
        ScraperResult remote = ScraperResult.failed(ScraperType.COMPANY_WEBSITE, "down");
        when(scraperResultCache.get(ScraperType.COMPANY_WEBSITE, context)).thenReturn(Optional.empty());
        when(scraperServiceClient.scrape(eq("http://localhost:8091"), eq(ScraperType.COMPANY_WEBSITE), eq(context)))
                .thenReturn(remote);

        ScraperResult result = scraper.scrape(context);

        assertThat(result.status()).isEqualTo(ScraperExecutionStatus.FAILED);
        verify(scraperResultCache, never()).put(any(), any(), any());
    }
}
