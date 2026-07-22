package com.datascraper.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "scraper")
public class IntelligenceScraperProperties {

    private final Resilience resilience = new Resilience();
    private final Map<String, ServiceEndpoint> services = new HashMap<>();

    public Resilience getResilience() {
        return resilience;
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
