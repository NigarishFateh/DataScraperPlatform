package com.datascraper.discovery.factory;

import com.datascraper.common.enums.DiscoveryProviderType;
import com.datascraper.common.provider.DiscoveryProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DiscoveryProviderRegistry {

    private final Map<DiscoveryProviderType, DiscoveryProvider> providersByType;

    public DiscoveryProviderRegistry(List<DiscoveryProvider> providers) {
        Map<DiscoveryProviderType, DiscoveryProvider> map = new EnumMap<>(DiscoveryProviderType.class);
        for (DiscoveryProvider provider : providers) {
            map.put(provider.type(), provider);
        }
        this.providersByType = Map.copyOf(map);
    }

    public DiscoveryProvider get(DiscoveryProviderType type) {
        DiscoveryProvider provider = providersByType.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("No discovery provider registered for type: " + type);
        }
        return provider;
    }

    public List<DiscoveryProvider> all() {
        return List.copyOf(providersByType.values());
    }
}
