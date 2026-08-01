package com.datascraper.common.provider;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.common.enums.DiscoveryProviderType;

import java.util.List;

/**
 * Plugin contract for company discovery. Implementations must not enrich data.
 * Adding a provider must not require changes to existing providers.
 */
public interface DiscoveryProvider {

    DiscoveryProviderType type();

    String name();

    boolean enabled();

    /**
     * Discover companies matching the request criteria.
     */
    List<DiscoveredCompany> discover(DiscoveryRequest request);
}
