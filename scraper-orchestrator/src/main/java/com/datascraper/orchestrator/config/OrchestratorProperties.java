package com.datascraper.orchestrator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class OrchestratorProperties {

    private String jobServiceUri = "http://localhost:8086";
    private String companyServiceUri = "http://localhost:8083";
    private String exportServiceUri = "http://localhost:8088";
    private final RedisProperties redis = new RedisProperties();
    private final QueueProperties queue = new QueueProperties();
    private final Map<String, ProviderToggle> providers = defaultProviders();

    @Getter
    @Setter
    public static class RedisProperties {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class QueueProperties {
        private boolean enabled = true;
        private long pollIntervalMs = 1000;
        private long blockTimeoutSeconds = 5;
    }

    @Getter
    @Setter
    public static class ProviderToggle {
        private boolean enabled = true;
    }

    public boolean isProviderEnabled(String key) {
        ProviderToggle toggle = providers.get(key);
        return toggle == null || toggle.isEnabled();
    }

    private static Map<String, ProviderToggle> defaultProviders() {
        Map<String, ProviderToggle> map = new HashMap<>();
        for (String key : new String[] {"website", "contact", "github", "technology", "news", "social"}) {
            ProviderToggle toggle = new ProviderToggle();
            toggle.setEnabled(true);
            map.put(key, toggle);
        }
        return map;
    }
}
