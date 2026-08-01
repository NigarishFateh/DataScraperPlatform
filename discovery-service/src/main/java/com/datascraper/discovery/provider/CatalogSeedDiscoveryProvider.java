package com.datascraper.discovery.provider;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.common.enums.DiscoveryProviderType;
import com.datascraper.common.provider.DiscoveryProvider;
import com.datascraper.discovery.config.DiscoveryProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogSeedDiscoveryProvider implements DiscoveryProvider {

    static final String PROVIDER_NAME = "Catalog Seed";

    private final DiscoveryProperties discoveryProperties;
    private final CatalogSearchSupport catalogSearchSupport;

    public CatalogSeedDiscoveryProvider(
            DiscoveryProperties discoveryProperties,
            CatalogSearchSupport catalogSearchSupport
    ) {
        this.discoveryProperties = discoveryProperties;
        this.catalogSearchSupport = catalogSearchSupport;
    }

    @Override
    public DiscoveryProviderType type() {
        return DiscoveryProviderType.CATALOG_SEED;
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean enabled() {
        return discoveryProperties.isEnabled(DiscoveryProviderType.CATALOG_SEED);
    }

    @Override
    public List<DiscoveredCompany> discover(DiscoveryRequest request) {
        if (!enabled()) {
            return List.of();
        }
        return catalogSearchSupport.paginateCatalog(request, name());
    }
}
