package com.datascraper.discovery.provider;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.common.enums.DiscoveryProviderType;
import com.datascraper.common.provider.DiscoveryProvider;
import com.datascraper.discovery.client.CategoryCatalogClient;
import com.datascraper.discovery.config.DiscoveryProperties;
import com.datascraper.discovery.dto.CategoryDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class IndustryListingDiscoveryProvider implements DiscoveryProvider {

    static final String PROVIDER_NAME = "Industry Listing";

    private final DiscoveryProperties discoveryProperties;
    private final CategoryCatalogClient categoryCatalogClient;
    private final CatalogSearchSupport catalogSearchSupport;

    public IndustryListingDiscoveryProvider(
            DiscoveryProperties discoveryProperties,
            CategoryCatalogClient categoryCatalogClient,
            CatalogSearchSupport catalogSearchSupport
    ) {
        this.discoveryProperties = discoveryProperties;
        this.categoryCatalogClient = categoryCatalogClient;
        this.catalogSearchSupport = catalogSearchSupport;
    }

    @Override
    public DiscoveryProviderType type() {
        return DiscoveryProviderType.INDUSTRY_LISTING;
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean enabled() {
        return discoveryProperties.isEnabled(DiscoveryProviderType.INDUSTRY_LISTING);
    }

    @Override
    public List<DiscoveredCompany> discover(DiscoveryRequest request) {
        if (!enabled()) {
            return List.of();
        }

        List<CategoryDto> categories = categoryCatalogClient.listByIds(request.categoryIds());
        if (categories.isEmpty()) {
            return List.of();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<DiscoveredCompany> discovered = new ArrayList<>();
        int remaining = request.maxResults();

        for (CategoryDto category : categories) {
            if (remaining <= 0) {
                break;
            }
            DiscoveryRequest categoryRequest = new DiscoveryRequest(
                    request.jobId(),
                    request.correlationId(),
                    request.countryCodes(),
                    request.cityIds(),
                    List.of(category.id()),
                    remaining,
                    request.companyNames()
            );
            for (DiscoveredCompany company : catalogSearchSupport.paginateCatalog(categoryRequest, name())) {
                String key = company.externalId() != null ? company.externalId() : company.name();
                if (seen.add(key)) {
                    discovered.add(company);
                    remaining--;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
        }

        return discovered;
    }
}
