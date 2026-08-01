package com.datascraper.discovery.config;

import com.datascraper.common.enums.DiscoveryProviderType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "discovery")
public class DiscoveryProperties {

    private Map<String, ProviderToggle> providers = defaultProviders();

    public boolean isEnabled(DiscoveryProviderType type) {
        String key = toConfigKey(type);
        ProviderToggle toggle = providers.get(key);
        if (toggle == null) {
            return defaultEnabled(type);
        }
        return toggle.isEnabled();
    }

    private static Map<String, ProviderToggle> defaultProviders() {
        Map<String, ProviderToggle> defaults = new java.util.HashMap<>();
        defaults.put("catalog-seed", new ProviderToggle(true));
        defaults.put("open-data", new ProviderToggle(true));
        defaults.put("industry-listing", new ProviderToggle(true));
        defaults.put("business-directory", new ProviderToggle(false));
        defaults.put("government-registry", new ProviderToggle(false));
        defaults.put("business-search-api", new ProviderToggle(false));
        return defaults;
    }

    private static boolean defaultEnabled(DiscoveryProviderType type) {
        return switch (type) {
            case CATALOG_SEED, OPEN_DATA, INDUSTRY_LISTING -> true;
            case BUSINESS_DIRECTORY, GOVERNMENT_REGISTRY, BUSINESS_SEARCH_API -> false;
        };
    }

    private static String toConfigKey(DiscoveryProviderType type) {
        return switch (type) {
            case CATALOG_SEED -> "catalog-seed";
            case OPEN_DATA -> "open-data";
            case INDUSTRY_LISTING -> "industry-listing";
            case BUSINESS_DIRECTORY -> "business-directory";
            case GOVERNMENT_REGISTRY -> "government-registry";
            case BUSINESS_SEARCH_API -> "business-search-api";
        };
    }

    @Getter
    @Setter
    public static class ProviderToggle {
        private boolean enabled;

        public ProviderToggle() {
        }

        public ProviderToggle(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
