/**
 * Wires either a no-op or Redis cache bean based on config.
 */
package com.datascraper.orchestrator.config;

import com.datascraper.orchestrator.cache.NoOpScraperResultCache;
import com.datascraper.orchestrator.cache.RedisScraperResultCache;
import com.datascraper.orchestrator.cache.ScraperResultCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class ScraperCacheConfig {

    @Bean
    @ConditionalOnProperty(name = "scraper.cache.enabled", havingValue = "false", matchIfMissing = true)
    public ScraperResultCache noOpScraperResultCache() {
        return new NoOpScraperResultCache();
    }

    @Bean
    @ConditionalOnProperty(name = "scraper.cache.enabled", havingValue = "true")
    public ScraperResultCache redisScraperResultCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            IntelligenceScraperProperties properties
    ) {
        return new RedisScraperResultCache(redisTemplate, objectMapper, properties);
    }
}
