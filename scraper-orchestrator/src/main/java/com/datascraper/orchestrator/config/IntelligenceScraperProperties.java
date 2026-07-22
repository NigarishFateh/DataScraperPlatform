package com.datascraper.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "scraper")
public class IntelligenceScraperProperties {

    private final Resilience resilience = new Resilience();
    private final Execution execution = new Execution();
    private final Map<String, ServiceEndpoint> services = new HashMap<>();

    public Resilience getResilience() {
        return resilience;
    }

    public Execution getExecution() {
        return execution;
    }

    public Map<String, ServiceEndpoint> getServices() {
        return services;
    }

    public static class Resilience {
        private long timeoutMs = 15000;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getRetryDelayMs() {
            return retryDelayMs;
        }

        public void setRetryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
        }
    }

    public static class Execution {
        private int corePoolSize = 4;
        private int maxPoolSize = 10;
        private int queueCapacity = 25;
        private long jobTimeoutMs = 60_000;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public long getJobTimeoutMs() {
            return jobTimeoutMs;
        }

        public void setJobTimeoutMs(long jobTimeoutMs) {
            this.jobTimeoutMs = jobTimeoutMs;
        }
    }

    public static class ServiceEndpoint {
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
