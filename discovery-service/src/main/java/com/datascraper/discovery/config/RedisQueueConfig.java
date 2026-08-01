package com.datascraper.discovery.config;

import com.datascraper.discovery.queue.DiscoveryQueuePort;
import com.datascraper.discovery.queue.InMemoryDiscoveryQueue;
import com.datascraper.discovery.queue.RedisDiscoveryQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisQueueConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
    public DiscoveryQueuePort redisDiscoveryQueue(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AppProperties appProperties
    ) {
        return new RedisDiscoveryQueue(redisTemplate, objectMapper, appProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
    public DiscoveryQueuePort inMemoryDiscoveryQueue(ObjectMapper objectMapper) {
        return new InMemoryDiscoveryQueue(objectMapper);
    }
}
