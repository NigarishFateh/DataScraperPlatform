/**
 * Holds all scraper strategy beans keyed by scraper type.
 */
package com.datascraper.orchestrator.factory;

import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.scraper.Scraper;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Spring-managed registry of all {@link Scraper} strategy beans.
 */
@Component
public class ScraperRegistry {

    private final Map<ScraperType, Scraper> scrapersByType;

    public ScraperRegistry(List<Scraper> scrapers) {
        Map<ScraperType, Scraper> map = new EnumMap<>(ScraperType.class);
        for (Scraper scraper : scrapers) {
            map.put(scraper.type(), scraper);
        }
        this.scrapersByType = Map.copyOf(map);
    }

    public Scraper get(ScraperType type) {
        Scraper scraper = scrapersByType.get(type);
        if (scraper == null) {
            throw new IllegalArgumentException("No scraper registered for type: " + type);
        }
        return scraper;
    }

    public List<Scraper> all() {
        return List.copyOf(scrapersByType.values());
    }
}
