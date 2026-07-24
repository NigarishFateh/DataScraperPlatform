/**
 * Tests that the factory picks the right scrapers for a job.
 */
package com.datascraper.orchestrator.factory;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.scraper.Scraper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScraperFactoryImplTest {

    private ScraperFactory scraperFactory;

    @BeforeEach
    void setUp() {
        Scraper website = mockScraper(ScraperType.COMPANY_WEBSITE, true);
        Scraper tech = mockScraper(ScraperType.TECHNOLOGY_STACK, true);
        Scraper news = mockScraper(ScraperType.NEWS, false);

        scraperFactory = new ScraperFactoryImpl(new ScraperRegistry(List.of(website, tech, news)));
    }

    @Test
    void resolveAllSupportedScrapersWhenTypesNotSpecified() {
        ScraperContext context = new ScraperContext(
                "job-1", "co-1", "Acme", "https://acme.example", List.of("cloud"), "corr-1");

        List<Scraper> selected = scraperFactory.resolve(context, null);

        assertThat(selected).hasSize(2);
        assertThat(selected).extracting(Scraper::type)
                .containsExactlyInAnyOrder(ScraperType.COMPANY_WEBSITE, ScraperType.TECHNOLOGY_STACK);
    }

    @Test
    void resolveOnlyRequestedTypes() {
        ScraperContext context = new ScraperContext(
                "job-1", "co-1", "Acme", "https://acme.example", List.of(), "corr-1");

        List<Scraper> selected = scraperFactory.resolve(context, List.of(ScraperType.TECHNOLOGY_STACK));

        assertThat(selected).hasSize(1);
        assertThat(selected.get(0).type()).isEqualTo(ScraperType.TECHNOLOGY_STACK);
    }

    private static Scraper mockScraper(ScraperType type, boolean supports) {
        Scraper scraper = mock(Scraper.class);
        when(scraper.type()).thenReturn(type);
        when(scraper.supports(org.mockito.ArgumentMatchers.any())).thenReturn(supports);
        when(scraper.scrape(org.mockito.ArgumentMatchers.any()))
                .thenReturn(ScraperResult.skipped(type, "test"));
        return scraper;
    }
}
