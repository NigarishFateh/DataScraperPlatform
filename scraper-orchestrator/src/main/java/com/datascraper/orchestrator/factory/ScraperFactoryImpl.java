package com.datascraper.orchestrator.factory;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.scraper.Scraper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScraperFactoryImpl implements ScraperFactory {

    private final ScraperRegistry scraperRegistry;

    public ScraperFactoryImpl(ScraperRegistry scraperRegistry) {
        this.scraperRegistry = scraperRegistry;
    }

    @Override
    public List<Scraper> resolve(ScraperContext context, List<ScraperType> requestedTypes) {
        List<Scraper> candidates = (requestedTypes == null || requestedTypes.isEmpty())
                ? scraperRegistry.all()
                : requestedTypes.stream().map(scraperRegistry::get).toList();

        return candidates.stream()
                .filter(scraper -> scraper.supports(context))
                .toList();
    }
}
