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
import java.util.Locale;
import java.util.Set;

@Component
public class OpenDataDiscoveryProvider implements DiscoveryProvider {

    static final String PROVIDER_NAME = "Open Data Heuristics";

    private final DiscoveryProperties discoveryProperties;
    private final CategoryCatalogClient categoryCatalogClient;
    private final CatalogSearchSupport catalogSearchSupport;

    public OpenDataDiscoveryProvider(
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
        return DiscoveryProviderType.OPEN_DATA;
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean enabled() {
        return discoveryProperties.isEnabled(DiscoveryProviderType.OPEN_DATA);
    }

    @Override
    public List<DiscoveredCompany> discover(DiscoveryRequest request) {
        if (!enabled()) {
            return List.of();
        }

        List<CategoryDto> categories = categoryCatalogClient.listByIds(request.categoryIds());
        Set<String> searchTerms = deriveSearchTerms(categories);
        if (searchTerms.isEmpty()) {
            return List.of();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<DiscoveredCompany> discovered = new ArrayList<>();
        int remaining = request.maxResults();

        for (String term : searchTerms) {
            if (remaining <= 0) {
                break;
            }
            DiscoveryRequest narrowed = new DiscoveryRequest(
                    request.jobId(),
                    request.correlationId(),
                    request.countryCodes(),
                    request.cityIds(),
                    request.categoryIds(),
                    remaining
            );
            for (DiscoveredCompany company : catalogSearchSupport.searchCatalog(narrowed, term, name())) {
                String key = company.website() != null && !company.website().isBlank()
                        ? company.website().toLowerCase(Locale.ROOT)
                        : company.name() + "|" + company.countryCode();
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

    public List<String> attemptSummaries(DiscoveryRequest request) {
        List<CategoryDto> categories = categoryCatalogClient.listByIds(request.categoryIds());
        return deriveSearchTerms(categories).stream().toList();
    }

    private Set<String> deriveSearchTerms(List<CategoryDto> categories) {
        Set<String> terms = new LinkedHashSet<>();
        for (CategoryDto category : categories) {
            if (category.name() == null || category.name().isBlank()) {
                continue;
            }
            terms.add(category.name().trim());
            for (String token : category.name().split("[\\s/&,-]+")) {
                if (token.length() >= 3) {
                    terms.add(token.trim());
                }
            }
        }
        return terms;
    }
}
