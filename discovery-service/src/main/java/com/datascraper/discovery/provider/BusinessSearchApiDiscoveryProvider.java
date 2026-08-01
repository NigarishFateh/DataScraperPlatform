package com.datascraper.discovery.provider;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.common.enums.DiscoveryProviderType;
import com.datascraper.common.provider.DiscoveryProvider;
import com.datascraper.discovery.config.DiscoveryProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BusinessSearchApiDiscoveryProvider implements DiscoveryProvider {

    static final String PROVIDER_NAME = "Business Search API";

    private final DiscoveryProperties discoveryProperties;

    public BusinessSearchApiDiscoveryProvider(DiscoveryProperties discoveryProperties) {
        this.discoveryProperties = discoveryProperties;
    }

    @Override
    public DiscoveryProviderType type() {
        return DiscoveryProviderType.BUSINESS_SEARCH_API;
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean enabled() {
        return discoveryProperties.isEnabled(DiscoveryProviderType.BUSINESS_SEARCH_API);
    }

    @Override
    public List<DiscoveredCompany> discover(DiscoveryRequest request) {
        return List.of();
    }
}
