package com.datascraper.discovery.service;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.discovery.client.DuckDuckGoSearchClient;
import com.datascraper.discovery.client.GitHubOrgDiscoveryClient;
import com.datascraper.discovery.client.WikidataDiscoveryClient;
import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import com.datascraper.discovery.dto.WebSearchHit;
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
    private final GitHubOrgDiscoveryClient gitHubOrgDiscoveryClient;
    private final WikidataDiscoveryClient wikidataDiscoveryClient;
    private final DuckDuckGoSearchClient duckDuckGoSearchClient;

    public WebCompanyDiscoveryService(
            DiscoveryCriteriaResolver criteriaResolver,
            GitHubOrgDiscoveryClient gitHubOrgDiscoveryClient,
            WikidataDiscoveryClient wikidataDiscoveryClient,
            DuckDuckGoSearchClient duckDuckGoSearchClient
    ) {
        this.criteriaResolver = criteriaResolver;
        this.gitHubOrgDiscoveryClient = gitHubOrgDiscoveryClient;
        this.wikidataDiscoveryClient = wikidataDiscoveryClient;
        this.duckDuckGoSearchClient = duckDuckGoSearchClient;
    }

    public List<DiscoveredCompany> discover(DiscoveryRequest request, String providerName) {
        ResolvedDiscoveryCriteria criteria = criteriaResolver.resolve(request);
        log.info(
                "Web discovery start categories={} countries={} cities={} keywords={} max={}",
                criteria.categoryNames(),
                criteria.countryCodes(),
                criteria.cityNames(),
                criteria.searchKeywords(),
                criteria.maxResults()
        );

        List<WebSearchHit> hits = new ArrayList<>();
        hits.addAll(safe(() -> gitHubOrgDiscoveryClient.discover(criteria), "github"));
        hits.addAll(safe(() -> wikidataDiscoveryClient.discover(criteria), "wikidata"));
        if (hits.size() < Math.min(10, criteria.maxResults())) {
            hits.addAll(safe(() -> duckDuckGoSearchClient.discover(criteria), "duckduckgo"));
        }

        Map<String, DiscoveredCompany> unique = new LinkedHashMap<>();
        for (WebSearchHit hit : hits) {
            DiscoveredCompany company = toDiscoveredCompany(hit, criteria, providerName);
            String key = dedupeKey(company);
            unique.putIfAbsent(key, company);
            if (unique.size() >= criteria.maxResults()) {
                break;
            }
        }

        log.info("Web discovery produced {} unique companies for provider {}", unique.size(), providerName);
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
        if (hit.cityName() != null) {
            metadata.put("cityName", hit.cityName());
        }

        String externalId = "web-" + UUID.nameUUIDFromBytes(
                (normalizeWebsite(hit.website()) + "|" + hit.name()).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        return new DiscoveredCompany(
                externalId.toString(),
                hit.name(),
                hit.website(),
                hit.countryCode(),
                hit.cityName(),
                hit.cityId(),
                criteria.categoryIds(),
                hit.sourceUrl() == null || hit.sourceUrl().isBlank() ? hit.website() : hit.sourceUrl(),
                providerName,
                metadata
        );
    }

    private static String dedupeKey(DiscoveredCompany company) {
        String website = normalizeWebsite(company.website());
        if (!website.isBlank()) {
            return website;
        }
        String name = company.name() == null ? "" : company.name().trim().toLowerCase(Locale.ROOT);
        String country = company.countryCode() == null ? "" : company.countryCode().trim().toUpperCase(Locale.ROOT);
        return name + "|" + country;
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
