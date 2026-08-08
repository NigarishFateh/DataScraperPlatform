package com.datascraper.discovery.service;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.discovery.client.BusinessSearchDiscoveryClient;
import com.datascraper.discovery.client.JobServiceClient;
import com.datascraper.discovery.dto.JobProgressPatchRequest;
import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import com.datascraper.discovery.dto.WebSearchHit;
import com.datascraper.discovery.support.NlRestaurantBrandSeed;
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
import java.util.function.IntConsumer;

@Service
public class WebCompanyDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(WebCompanyDiscoveryService.class);

    private final DiscoveryCriteriaResolver criteriaResolver;
    private final BusinessSearchDiscoveryClient businessSearchDiscoveryClient;
    private final JobServiceClient jobServiceClient;
    private final NlRestaurantLeadershipService leadershipService;

    public WebCompanyDiscoveryService(
            DiscoveryCriteriaResolver criteriaResolver,
            BusinessSearchDiscoveryClient businessSearchDiscoveryClient,
            JobServiceClient jobServiceClient,
            NlRestaurantLeadershipService leadershipService
    ) {
        this.criteriaResolver = criteriaResolver;
        this.businessSearchDiscoveryClient = businessSearchDiscoveryClient;
        this.jobServiceClient = jobServiceClient;
        this.leadershipService = leadershipService;
    }

    public List<DiscoveredCompany> discover(DiscoveryRequest request, String providerName) {
        List<String> categoryIds = request.categoryIds() == null ? List.of() : request.categoryIds();
        boolean namedMode = request.companyNames() != null && !request.companyNames().isEmpty();

        // Multi-category jobs discover each category separately so exports can partition cleanly.
        if (!namedMode && categoryIds.size() > 1) {
            Map<String, DiscoveredCompany> unique = new LinkedHashMap<>();
            int remaining = request.maxResults() <= 0 ? 500 : request.maxResults();
            for (String categoryId : categoryIds) {
                if (categoryId == null || categoryId.isBlank() || remaining <= 0) {
                    continue;
                }
                DiscoveryRequest singleCategory = new DiscoveryRequest(
                        request.jobId(),
                        request.correlationId(),
                        request.countryCodes(),
                        request.cityIds(),
                        List.of(categoryId),
                        remaining,
                        request.companyNames()
                );
                for (DiscoveredCompany company : discoverSingleCategory(singleCategory, providerName)) {
                    unique.putIfAbsent(dedupeKey(company), company);
                    if (unique.size() >= request.maxResults()) {
                        notifyLiveCount(request, unique.size());
                        return new ArrayList<>(unique.values());
                    }
                }
                notifyLiveCount(request, unique.size());
                remaining = Math.max(0, request.maxResults() - unique.size());
            }
            log.info(
                    "Multi-category web discovery produced {} unique companies across {} categories",
                    unique.size(),
                    categoryIds.size()
            );
            return new ArrayList<>(unique.values());
        }

        return discoverSingleCategory(request, providerName);
    }

    private List<DiscoveredCompany> discoverSingleCategory(DiscoveryRequest request, String providerName) {
        ResolvedDiscoveryCriteria criteria = criteriaResolver.resolve(request);
        boolean namedMode = criteria.hasCompanyNames();
        boolean businessSearchReady = businessSearchDiscoveryClient.isConfigured();

        log.info(
                "Web discovery start named={} companies={} categories={} countries={} cities={} (priority-first nationwide when empty) keywords={} businessSearch={} max={}",
                namedMode,
                criteria.companyNames(),
                criteria.categoryNames(),
                criteria.countryCodes(),
                criteria.cityNames().size(),
                criteria.searchKeywords(),
                businessSearchReady ? businessSearchDiscoveryClient.configuredProviders() : "disabled",
                criteria.maxResults()
        );

        // Named Custom scrape: always emit one company per requested name (brand seed + Apollo),
        // independent of category keyword / industry filters.
        if (namedMode) {
            List<DiscoveredCompany> named = discoverNamedCompanies(request, criteria, providerName);
            notifyLiveCount(request, named.size());
            log.info("Named web discovery produced {} companies for provider {}", named.size(), providerName);
            return named;
        }

        if (!businessSearchReady) {
            log.warn(
                    "No business-search API key configured. Set APOLLO_API_KEY, GOOGLE_PLACES_API_KEY, "
                            + "and/or SERPAPI_API_KEY in .env for company discovery."
            );
        }

        List<WebSearchHit> hits = new ArrayList<>();
        if (businessSearchReady) {
            IntConsumer onProgress = running -> notifyLiveCount(request, Math.min(running, criteria.maxResults()));
            hits.addAll(safe(
                    () -> businessSearchDiscoveryClient.discover(criteria, onProgress),
                    "business-search"
            ));
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

        notifyLiveCount(request, unique.size());

        log.info(
                "Web discovery produced {} unique companies for provider {} (rejected={} irrelevant)",
                unique.size(),
                providerName,
                rejected
        );
        return new ArrayList<>(unique.values());
    }

    /**
     * Custom scrape path: one row per requested company name.
     * Prefer official brand websites, then Apollo org name search.
     */
    private List<DiscoveredCompany> discoverNamedCompanies(
            DiscoveryRequest request,
            ResolvedDiscoveryCriteria criteria,
            String providerName
    ) {
        Map<String, DiscoveredCompany> unique = new LinkedHashMap<>();

        for (WebSearchHit seed : seedNamedCompanyHits(criteria)) {
            DiscoveredCompany company = toDiscoveredCompany(seed, criteria, providerName);
            unique.putIfAbsent(dedupeKey(company), company);
            notifyLiveCount(request, unique.size());
        }

        if (businessSearchDiscoveryClient.isConfigured()) {
            IntConsumer onProgress = running -> notifyLiveCount(
                    request,
                    Math.max(unique.size(), Math.min(running, criteria.maxResults()))
            );
            for (WebSearchHit hit : safe(
                    () -> businessSearchDiscoveryClient.discover(criteria, onProgress),
                    "business-search"
            )) {
                DiscoveredCompany company = toDiscoveredCompany(hit, criteria, providerName);
                unique.putIfAbsent(dedupeKey(company), company);
                notifyLiveCount(request, unique.size());
                if (unique.size() >= criteria.maxResults()) {
                    break;
                }
            }
        }

        // Guarantee a stub for every requested name so custom scrape discovers the full list.
        for (String rawName : criteria.companyNames()) {
            if (unique.size() >= criteria.maxResults()) {
                break;
            }
            String canonical = NlRestaurantBrandSeed.canonicalBrandName(rawName);
            boolean already = unique.values().stream().anyMatch(c ->
                    c.name() != null && c.name().equalsIgnoreCase(canonical));
            if (already) {
                continue;
            }
            String website = NlRestaurantBrandSeed.officialWebsite(canonical);
            if (!WebsiteUrlSupport.isUsableCompanyWebsite(website)) {
                log.warn(
                        "Named company '{}' has no seeded website — emitting discovery stub for enrichment attempt",
                        canonical
                );
                website = null;
            }
            String country = criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);
            WebSearchHit stub = new WebSearchHit(
                    canonical,
                    website,
                    website,
                    country,
                    null,
                    null,
                    "brand-seed"
            );
            unique.putIfAbsent(dedupeKey(toDiscoveredCompany(stub, criteria, providerName)),
                    toDiscoveredCompany(stub, criteria, providerName));
        }

        List<DiscoveredCompany> companies = new ArrayList<>(unique.values());
        return attachLeadership(companies);
    }

    /**
     * Enrich named discoveries with CEO / founder so job Excel "Founder Name" is filled.
     */
    private List<DiscoveredCompany> attachLeadership(List<DiscoveredCompany> companies) {
        List<DiscoveredCompany> enriched = new ArrayList<>(companies.size());
        for (DiscoveredCompany company : companies) {
            enriched.add(withLeadershipMetadata(company));
        }
        return enriched;
    }

    private DiscoveredCompany withLeadershipMetadata(DiscoveredCompany company) {
        if (company == null || company.name() == null || company.name().isBlank()) {
            return company;
        }
        try {
            var lead = leadershipService.lookupOne(company.name());
            if (lead == null || !lead.found() || lead.leaderName() == null || lead.leaderName().isBlank()) {
                return company;
            }
            Map<String, Object> metadata = new HashMap<>();
            if (company.metadata() != null) {
                metadata.putAll(company.metadata());
            }
            String title = lead.leadershipTitle() == null ? "" : lead.leadershipTitle().trim();
            String display = title.isBlank()
                    ? lead.leaderName().trim()
                    : lead.leaderName().trim() + " (" + title + ")";
            metadata.put("leadershipName", lead.leaderName().trim());
            metadata.put("leadershipTitle", title.isBlank() ? null : title);
            metadata.put("leadershipSource", lead.source());

            String titleLower = title.toLowerCase(Locale.ROOT);
            if (titleLower.contains("founder") || titleLower.contains("owner") || titleLower.contains("co-founder")) {
                metadata.put("founder", display);
            } else {
                metadata.put("ceo", display);
                // Excel prefers founder then ceo — keep both so Founder Name always fills.
                metadata.putIfAbsent("founder", display);
            }
            log.info(
                    "Named scrape leadership {} -> {} via {}",
                    company.name(),
                    display,
                    lead.source()
            );
            return new DiscoveredCompany(
                    company.externalId(),
                    company.name(),
                    company.website(),
                    company.countryCode(),
                    company.cityName(),
                    company.cityId(),
                    company.categoryIds(),
                    company.sourceUrl(),
                    company.providerName(),
                    metadata
            );
        } catch (Exception ex) {
            log.warn("Leadership attach failed for {}: {}", company.name(), ex.getMessage());
            return company;
        }
    }

    private void notifyLiveCount(DiscoveryRequest request, int count) {
        if (count <= 0 || request.jobId() == null || request.jobId().isBlank()) {
            return;
        }
        try {
            UUID jobId = UUID.fromString(request.jobId());
            jobServiceClient.patchProgress(jobId, JobProgressPatchRequest.discoveredProgress(count));
        } catch (Exception ex) {
            log.debug("Live discovered count patch skipped: {}", ex.getMessage());
        }
    }

    private List<WebSearchHit> seedNamedCompanyHits(ResolvedDiscoveryCriteria criteria) {
        List<WebSearchHit> seeded = new ArrayList<>();
        String country = criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);
        for (String rawName : criteria.companyNames()) {
            String canonical = NlRestaurantBrandSeed.canonicalBrandName(rawName);
            String website = NlRestaurantBrandSeed.officialWebsite(canonical);
            if (website == null || website.isBlank()) {
                continue;
            }
            seeded.add(new WebSearchHit(
                    canonical,
                    website,
                    website,
                    country,
                    null,
                    null,
                    "brand-seed"
            ));
        }
        return seeded;
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

        // Named scrapes: stamp a single category so multi-sheet Excel doesn't collapse onto sheet 1.
        List<String> categoryIds = criteria.categoryIds();
        if (criteria.hasCompanyNames() && categoryIds.size() > 1) {
            categoryIds = List.of(categoryIds.get(0));
        }

        return new DiscoveredCompany(
                externalId,
                hit.name(),
                hit.website(),
                hit.countryCode(),
                cityName,
                cityId,
                categoryIds,
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
