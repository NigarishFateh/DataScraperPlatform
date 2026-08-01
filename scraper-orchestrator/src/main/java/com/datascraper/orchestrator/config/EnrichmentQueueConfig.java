package com.datascraper.orchestrator.config;

import com.datascraper.orchestrator.queue.EnrichmentQueuePort;
import com.datascraper.orchestrator.queue.InMemoryEnrichmentQueue;
import com.datascraper.orchestrator.queue.RedisEnrichmentQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class EnrichmentQueueConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
    public EnrichmentQueuePort redisEnrichmentQueue(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            OrchestratorProperties properties
    ) {
        return new RedisEnrichmentQueue(redisTemplate, objectMapper, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
    public EnrichmentQueuePort inMemoryEnrichmentQueue() {
        return new InMemoryEnrichmentQueue();
    }
}
