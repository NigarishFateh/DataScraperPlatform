package com.datascraper.discovery.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String companyServiceUri = "http://localhost:8083";
    private String categoryServiceUri = "http://localhost:8084";
    private String jobServiceUri = "http://localhost:8086";
    private String locationServiceUri = "http://localhost:8082";
    private String orchestratorServiceUri = "http://localhost:8085";
    private RedisProperties redis = new RedisProperties();
    private QueueProperties queue = new QueueProperties();
    private EnrichmentProperties enrichment = new EnrichmentProperties();
    private String githubToken = "";
    private String googlePlacesApiKey = "";
    private String serpapiApiKey = "";
    private String apolloApiKey = "";
    /** Financial Modeling Prep — Company Executives (public tickers). */
    private String fmpApiKey = "";

    @Getter
    @Setter
    public static class RedisProperties {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class QueueProperties {
        private long pollIntervalMs = 1000;
        private long blockTimeoutSeconds = 5;
    }

    /**
     * Caps how many companies discovery may enrich in parallel over HTTP
     * (used when Redis enrichment queue is disabled).
     */
    @Getter
    @Setter
    public static class EnrichmentProperties {
        /** Parallel HTTP enrich calls. Clamped to 1–16 at runtime. */
        private int dispatchConcurrency = 8;
        /** Bounded queue before CallerRuns backpressure kicks in. */
        private int dispatchQueueCapacity = 64;
    }
}
