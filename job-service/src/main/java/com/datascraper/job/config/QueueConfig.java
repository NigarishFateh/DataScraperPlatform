package com.datascraper.job.config;

import com.datascraper.common.dto.messaging.DiscoveryQueueMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentLinkedQueue;

@Configuration
public class QueueConfig {

    @Bean
    ConcurrentLinkedQueue<DiscoveryQueueMessage> discoveryInMemoryQueue() {
        return new ConcurrentLinkedQueue<>();
    }
}
