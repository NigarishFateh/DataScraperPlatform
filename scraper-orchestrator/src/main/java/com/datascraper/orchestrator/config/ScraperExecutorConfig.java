/**
 * Creates the thread pool used to run scrapers in parallel.
 */
package com.datascraper.orchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ScraperExecutorConfig {

    @Bean(name = "scraperExecutor")
    public Executor scraperExecutor(IntelligenceScraperProperties properties) {
        IntelligenceScraperProperties.Execution execution = properties.getExecution();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(execution.getCorePoolSize());
        executor.setMaxPoolSize(execution.getMaxPoolSize());
        executor.setQueueCapacity(execution.getQueueCapacity());
        executor.setThreadNamePrefix("scraper-");
        executor.initialize();
        return executor;
    }

}
