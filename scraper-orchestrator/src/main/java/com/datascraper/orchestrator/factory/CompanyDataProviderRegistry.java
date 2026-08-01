package com.datascraper.orchestrator.factory;

import com.datascraper.common.enums.ProviderType;
import com.datascraper.common.provider.CompanyDataProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class CompanyDataProviderRegistry {

    private final Map<ProviderType, CompanyDataProvider> providersByType;

    public CompanyDataProviderRegistry(List<CompanyDataProvider> providers) {
        Map<ProviderType, CompanyDataProvider> map = new EnumMap<>(ProviderType.class);
        for (CompanyDataProvider provider : providers) {
            map.put(provider.type(), provider);
        }
        this.providersByType = Map.copyOf(map);
    }

    public CompanyDataProvider get(ProviderType type) {
        CompanyDataProvider provider = providersByType.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("No provider registered for type: " + type);
        }
        return provider;
    }

    public List<CompanyDataProvider> all() {
        return List.copyOf(providersByType.values());
    }
}
