package com.datascraper.discovery.provider;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.discovery.client.CompanyCatalogClient;
import com.datascraper.discovery.client.LocationCatalogClient;
import com.datascraper.discovery.dto.CompanyDto;
import com.datascraper.discovery.dto.CompanyPageDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class CatalogSearchSupport {

    private final CompanyCatalogClient companyCatalogClient;
    private final LocationCatalogClient locationCatalogClient;

    public CatalogSearchSupport(
            CompanyCatalogClient companyCatalogClient,
            LocationCatalogClient locationCatalogClient
    ) {
        this.companyCatalogClient = companyCatalogClient;
        this.locationCatalogClient = locationCatalogClient;
    }

    public List<DiscoveredCompany> searchCatalog(
            DiscoveryRequest request,
            String searchTerm,
            String providerName
    ) {
        List<String> cityIds = locationCatalogClient.resolveCityIds(request.cityIds(), request.countryCodes());

        int remaining = request.maxResults();
        int page = 0;
        int pageSize = Math.min(50, Math.max(remaining, 1));
        Set<String> seenKeys = new HashSet<>();
        List<DiscoveredCompany> results = new ArrayList<>();

        while (remaining > 0) {
            CompanyPageDto pageResult = companyCatalogClient.search(
                    cityIds,
                    searchTerm,
                    request.categoryIds(),
                    page,
                    Math.min(pageSize, remaining)
            );

            for (CompanyDto company : pageResult.items()) {
                if (!matchesCountryFilter(company, request.countryCodes())) {
                    continue;
                }
                DiscoveredCompany discovered = toDiscoveredCompany(company, providerName);
                String key = dedupeKey(discovered);
                if (seenKeys.add(key)) {
                    results.add(discovered);
                    remaining--;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }

            if (!pageResult.hasMore() || pageResult.items().isEmpty()) {
                break;
            }
            page++;
        }

        return results;
    }

    public List<DiscoveredCompany> paginateCatalog(
            DiscoveryRequest request,
            String providerName
    ) {
        return searchCatalog(request, "", providerName);
    }

    private boolean matchesCountryFilter(CompanyDto company, List<String> countryCodes) {
        if (countryCodes == null || countryCodes.isEmpty()) {
            return true;
        }
        if (company.countryCode() == null || company.countryCode().isBlank()) {
            return false;
        }
        String normalized = company.countryCode().trim().toUpperCase(Locale.ROOT);
        return countryCodes.stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .anyMatch(code -> code.equals(normalized));
    }

    public DiscoveredCompany toDiscoveredCompany(CompanyDto company, String providerName) {
        return new DiscoveredCompany(
                company.id(),
                company.name(),
                company.website(),
                company.countryCode(),
                null,
                company.cityId(),
                company.categoryIds(),
                company.website(),
                providerName,
                java.util.Map.of("industry", company.industry() == null ? "" : company.industry())
        );
    }

    private String dedupeKey(DiscoveredCompany company) {
        if (company.website() != null && !company.website().isBlank()) {
            return normalizeWebsite(company.website());
        }
        String name = company.name() == null ? "" : company.name().trim().toLowerCase(Locale.ROOT);
        String country = company.countryCode() == null ? "" : company.countryCode().trim().toUpperCase(Locale.ROOT);
        return name + "|" + country;
    }

    private String normalizeWebsite(String website) {
        String normalized = website.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceFirst("^https?://", "");
        normalized = normalized.replaceFirst("^www\\.", "");
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
