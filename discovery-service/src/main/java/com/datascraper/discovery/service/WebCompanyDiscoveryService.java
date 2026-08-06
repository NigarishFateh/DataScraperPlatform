package com.datascraper.discovery.service;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.discovery.client.BusinessSearchDiscoveryClient;
import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import com.datascraper.discovery.dto.WebSearchHit;
import com.datascraper.discovery.support.WebsiteUrlSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class WebCompanyDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(WebCompanyDiscoveryService.class);

    private final DiscoveryCriteriaResolver criteriaResolver;
    private final BusinessSearchDiscoveryClient businessSearchDiscoveryClient;

    public WebCompanyDiscoveryService(
            DiscoveryCriteriaResolver criteriaResolver,
            BusinessSearchDiscoveryClient businessSearchDiscoveryClient
    ) {
        this.criteriaResolver = criteriaResolver;
        this.businessSearchDiscoveryClient = businessSearchDiscoveryClient;
    }

    public List<DiscoveredCompany> discover(DiscoveryRequest request, String providerName) {
        ResolvedDiscoveryCriteria criteria = criteriaResolver.resolve(request);
        boolean businessSearchReady = businessSearchDiscoveryClient.isConfigured();

        log.info(
                "Web discovery start categories={} countries={} cities={} keywords={} businessSearch={} max={}",
                criteria.categoryNames(),
                criteria.countryCodes(),
                criteria.cityNames(),
                criteria.searchKeywords(),
                businessSearchReady ? businessSearchDiscoveryClient.configuredProviders() : "disabled",
                criteria.maxResults()
        );

        if (!businessSearchReady) {
            log.warn(
                    "No business-search API key configured. Set APOLLO_API_KEY, GOOGLE_PLACES_API_KEY, "
                            + "and/or SERPAPI_API_KEY in .env for company discovery."
            );
        }

        List<WebSearchHit> hits = new ArrayList<>();

        if (businessSearchReady) {
            hits.addAll(safe(() -> businessSearchDiscoveryClient.discover(criteria), "business-search"));
        }

        Map<String, DiscoveredCompany> unique = new LinkedHashMap<>();
        int rejected = 0;
        for (WebSearchHit hit : hits) {
            if (!WebsiteUrlSupport.isUsableCompanyWebsite(hit.website())) {
                rejected++;
                continue;
            }
            if (!CategoryDiscoverySupport.matchesIndustry(hit, criteria)) {
                rejected++;
                continue;
            }
            DiscoveredCompany company = toDiscoveredCompany(hit, criteria, providerName);
            String key = dedupeKey(company);
            unique.putIfAbsent(key, company);
            if (unique.size() >= criteria.maxResults()) {
                break;
            }
        }

        log.info(
                "Web discovery produced {} unique companies for provider {} (rejected={} irrelevant)",
                unique.size(),
                providerName,
                rejected
        );
        return new ArrayList<>(unique.values());
    }

    private List<WebSearchHit> safe(SourceCall call, String source) {
        try {
            return call.get();
        } catch (Exception ex) {
            log.warn("Discovery source '{}' failed: {}", source, ex.getMessage());
            return List.of();
        }
    }

    private DiscoveredCompany toDiscoveredCompany(
            WebSearchHit hit,
            ResolvedDiscoveryCriteria criteria,
            String providerName
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("discoverySource", hit.providerSource());
        metadata.put("searchKeywords", String.join(", ", criteria.searchKeywords()));
        String cityName = firstNonBlank(hit.cityName(), firstCityName(criteria));
        String cityId = firstNonBlank(hit.cityId(), firstCityId(criteria));
        if (cityName != null) {
            metadata.put("cityName", cityName);
        }
        if (cityId != null) {
            metadata.put("cityId", cityId);
        }

        String externalId = "web-" + UUID.nameUUIDFromBytes(
                (normalizeWebsite(hit.website()) + "|" + hit.name()).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        return new DiscoveredCompany(
                externalId,
                hit.name(),
                hit.website(),
                hit.countryCode(),
                cityName,
                cityId,
                criteria.categoryIds(),
                hit.sourceUrl() == null || hit.sourceUrl().isBlank() ? hit.website() : hit.sourceUrl(),
                providerName,
                metadata
        );
    }

    private static String firstCityId(ResolvedDiscoveryCriteria criteria) {
        return criteria.cityIds().isEmpty() ? null : criteria.cityIds().get(0);
    }

    private static String firstCityName(ResolvedDiscoveryCriteria criteria) {
        return criteria.cityNames().isEmpty() ? null : criteria.cityNames().get(0);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private static String dedupeKey(DiscoveredCompany company) {
        String website = normalizeWebsite(company.website());
        if (!website.isBlank()) {
            return website;
        }
        String name = company.name() == null ? "" : company.name().trim().toLowerCase(Locale.ROOT);
        String country = company.countryCode() == null ? "" : company.countryCode().trim().toUpperCase(Locale.ROOT);
        String city = company.cityName() == null ? "" : company.cityName().trim().toLowerCase(Locale.ROOT);
        return name + "|" + country + "|" + city;
    }

    private static String normalizeWebsite(String website) {
        if (website == null || website.isBlank()) {
            return "";
        }
        String normalized = website.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceFirst("^https?://", "");
        normalized = normalized.replaceFirst("^www\\.", "");
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @FunctionalInterface
    private interface SourceCall {
        List<WebSearchHit> get() throws Exception;
    }
}
