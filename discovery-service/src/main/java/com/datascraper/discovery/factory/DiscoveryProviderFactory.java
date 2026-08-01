package com.datascraper.discovery.factory;

import com.datascraper.common.enums.DiscoveryProviderType;
import com.datascraper.common.provider.DiscoveryProvider;
import com.datascraper.discovery.dto.ProviderInfoResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class DiscoveryProviderFactory {

    private final DiscoveryProviderRegistry registry;

    public DiscoveryProviderFactory(DiscoveryProviderRegistry registry) {
        this.registry = registry;
    }

    public List<DiscoveryProvider> getEnabledProviders(List<String> requestedProviders) {
        List<DiscoveryProvider> candidates = registry.all().stream()
                .filter(DiscoveryProvider::enabled)
                .toList();

        if (requestedProviders == null || requestedProviders.isEmpty()) {
            return candidates;
        }

        List<String> normalized = requestedProviders.stream()
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .toList();

        return candidates.stream()
                .filter(provider -> normalized.contains(provider.type().name())
                        || normalized.contains(provider.name().toUpperCase(Locale.ROOT)))
                .toList();
    }

    public List<ProviderInfoResponse> listProviders() {
        return registry.all().stream()
                .map(provider -> new ProviderInfoResponse(provider.type(), provider.name(), provider.enabled()))
                .toList();
    }

    public DiscoveryProvider get(DiscoveryProviderType type) {
        return registry.get(type);
    }
}
