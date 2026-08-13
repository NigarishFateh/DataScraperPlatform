package com.datascraper.orchestrator.cache;

import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * In-process memo so six FEBO branches sharing febo.nl scrape the site once, not six times.
 */
@Component
public class SharedWebsiteScrapeMemo {

    private final ConcurrentMap<String, ScraperResult> completed = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    public ScraperResult getOrLoad(ScraperType type, String websiteUrl, Supplier<ScraperResult> loader) {
        String key = type.name() + ":" + ScraperCacheKeyBuilder.normalizeUrl(websiteUrl);
        ScraperResult cached = completed.get(key);
        if (cached != null) {
            return cached;
        }
        Object lock = locks.computeIfAbsent(key, ignored -> new Object());
        synchronized (lock) {
            cached = completed.get(key);
            if (cached != null) {
                return cached;
            }
            ScraperResult loaded = loader.get();
            if (loaded != null) {
                completed.put(key, loaded);
            }
            return loaded;
        }
    }
}
