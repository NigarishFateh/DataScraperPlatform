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
        // Catalog-only discovery disabled until real web discovery providers are implemented.
        defaults.put("catalog-seed", new ProviderToggle(false));
        defaults.put("open-data", new ProviderToggle(false));
        defaults.put("industry-listing", new ProviderToggle(false));
        defaults.put("business-directory", new ProviderToggle(false));
        defaults.put("government-registry", new ProviderToggle(false));
        defaults.put("business-search-api", new ProviderToggle(true));
        return defaults;
    }

    private static boolean defaultEnabled(DiscoveryProviderType type) {
        return type == DiscoveryProviderType.BUSINESS_SEARCH_API;
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
