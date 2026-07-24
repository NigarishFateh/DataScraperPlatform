/**
 * Stores and loads scraper results in Redis with a time-to-live.
 */
package com.datascraper.orchestrator.cache;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperExecutionStatus;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class RedisScraperResultCache implements ScraperResultCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final IntelligenceScraperProperties properties;

    public RedisScraperResultCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            IntelligenceScraperProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Optional<ScraperResult> get(ScraperType scraperType, ScraperContext context) {
        String key = ScraperCacheKeyBuilder.build(properties.getCache().getKeyPrefix(), scraperType, context);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            ScraperResult result = objectMapper.readValue(json, ScraperResult.class);
            log.debug("Cache HIT for {} company={} key={}", scraperType, context.companyId(), key);
            return Optional.of(withCacheMetadata(result));
        } catch (Exception ex) {
            log.warn("Cache read failed for {} company={}: {}", scraperType, context.companyId(), ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(ScraperType scraperType, ScraperContext context, ScraperResult result) {
        if (result.status() != ScraperExecutionStatus.SUCCESS) {
            return;
        }

        String key = ScraperCacheKeyBuilder.build(properties.getCache().getKeyPrefix(), scraperType, context);
        try {
            String json = objectMapper.writeValueAsString(stripCacheMetadata(result));
            Duration ttl = Duration.ofSeconds(properties.getCache().getTtlSeconds());
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Cache PUT for {} company={} ttl={}s key={}",
                    scraperType, context.companyId(), ttl.toSeconds(), key);
        } catch (JsonProcessingException ex) {
            log.warn("Cache write failed for {} company={}: {}", scraperType, context.companyId(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("Cache write failed for {} company={}: {}", scraperType, context.companyId(), ex.getMessage());
        }
    }

    private static ScraperResult withCacheMetadata(ScraperResult result) {
        Map<String, Object> metadata = new HashMap<>(result.metadata() != null ? result.metadata() : Map.of());
        metadata.put("fromCache", true);
        return new ScraperResult(
                result.scraperType(),
                result.status(),
                result.message(),
                result.scrapedAt(),
                result.items(),
                Map.copyOf(metadata)
        );
    }

    private static ScraperResult stripCacheMetadata(ScraperResult result) {
        if (result.metadata() == null || !result.metadata().containsKey("fromCache")) {
            return result;
        }
        Map<String, Object> metadata = new HashMap<>(result.metadata());
        metadata.remove("fromCache");
        return new ScraperResult(
                result.scraperType(),
                result.status(),
                result.message(),
                result.scrapedAt(),
                result.items(),
                Map.copyOf(metadata)
        );
    }
}
