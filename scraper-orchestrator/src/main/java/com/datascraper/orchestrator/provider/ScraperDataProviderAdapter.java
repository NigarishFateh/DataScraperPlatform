package com.datascraper.orchestrator.provider;

import com.datascraper.common.dto.provider.ProviderContext;
import com.datascraper.common.dto.provider.ProviderResult;
import com.datascraper.common.enums.ProviderType;
import com.datascraper.common.provider.CompanyDataProvider;
import com.datascraper.orchestrator.scraper.Scraper;
import com.datascraper.orchestrator.support.ProviderResultMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ScraperDataProviderAdapter implements CompanyDataProvider {

    private final Scraper scraper;
    private final ProviderType providerType;
    private final String providerName;
    private final boolean enabled;

    protected ScraperDataProviderAdapter(
            Scraper scraper,
            ProviderType providerType,
            String providerName,
            boolean enabled
    ) {
        this.scraper = scraper;
        this.providerType = providerType;
        this.providerName = providerName;
        this.enabled = enabled;
    }

    @Override
    public ProviderType type() {
        return providerType;
    }

    @Override
    public String name() {
        return providerName;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public ProviderResult enrich(ProviderContext context) {
        var scraperContext = ProviderResultMapper.toScraperContext(context);
        if (!scraper.supports(scraperContext)) {
            return ProviderResult.skipped(providerType, providerName, "Context not supported for " + providerType);
        }
        try {
            var scraperResult = scraper.scrape(scraperContext);
            return ProviderResultMapper.fromScraperResult(scraperResult, providerType, providerName);
        } catch (Exception ex) {
            log.error("Provider {} failed for job {} company {}", providerType, context.jobId(), context.companyId(), ex);
            return ProviderResult.failed(providerType, providerName,
                    ex.getMessage() != null ? ex.getMessage() : "Unexpected provider error");
        }
    }
}
