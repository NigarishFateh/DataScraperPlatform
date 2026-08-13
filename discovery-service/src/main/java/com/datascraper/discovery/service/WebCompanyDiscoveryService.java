package com.datascraper.discovery.service;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.discovery.client.BusinessSearchDiscoveryClient;
import com.datascraper.discovery.client.GooglePlacesDiscoveryClient;
import com.datascraper.discovery.client.JobServiceClient;
import com.datascraper.discovery.dto.JobProgressPatchRequest;
import com.datascraper.discovery.dto.LeadershipPersonResponse;
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
    private final GooglePlacesDiscoveryClient googlePlacesDiscoveryClient;
    private final JobServiceClient jobServiceClient;
    private final NlRestaurantLeadershipService leadershipService;

    public WebCompanyDiscoveryService(
            DiscoveryCriteriaResolver criteriaResolver,
            BusinessSearchDiscoveryClient businessSearchDiscoveryClient,
            GooglePlacesDiscoveryClient googlePlacesDiscoveryClient,
            JobServiceClient jobServiceClient,
            NlRestaurantLeadershipService leadershipService
    ) {
        this.criteriaResolver = criteriaResolver;
        this.businessSearchDiscoveryClient = businessSearchDiscoveryClient;
        this.googlePlacesDiscoveryClient = googlePlacesDiscoveryClient;
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
        IntConsumer onProgress = running -> notifyLiveCount(request, Math.min(running, criteria.maxResults()));
        if (businessSearchReady) {
            hits.addAll(safe(
                    () -> businessSearchDiscoveryClient.discover(criteria, onProgress),
                    "business-search"
            ));
        }

        Map<String, DiscoveredCompany> unique = new LinkedHashMap<>();
        int rejected = 0;
        rejected += collectCompanies(hits, criteria, providerName, unique);

        notifyLiveCount(request, unique.size());

        if (unique.isEmpty()
                && !criteria.cityIds().isEmpty()
                && !criteria.countryCodes().isEmpty()
                && businessSearchReady) {
            ResolvedDiscoveryCriteria nationwide = criteriaResolver.expandToMajorCities(criteria);
            log.info(
                    "No companies in cities {} — retrying major cities {}",
                    criteria.cityNames(),
                    nationwide.cityNames()
            );
            hits = safe(
                    () -> businessSearchDiscoveryClient.discover(nationwide, onProgress),
                    "business-search-nationwide"
            );
            rejected = collectCompanies(hits, nationwide, providerName, unique);
            notifyLiveCount(request, unique.size());
        }

        log.info(
                "Web discovery produced {} unique companies for provider {} (rejected={} irrelevant)",
                unique.size(),
                providerName,
                rejected
        );
        return new ArrayList<>(unique.values());
    }

    /**
     * Custom scrape path: Apollo org hits + Places branch expansion per brand name.
     * Prefer official brand websites, then Apollo, then every Places branch location.
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

        // Places branch expansion only when the job asked for more rows than brand names.
        boolean expandBranches = criteria.maxResults() > criteria.companyNames().size();
        if (expandBranches && unique.size() < criteria.maxResults() && googlePlacesDiscoveryClient.isConfigured()) {
            for (WebSearchHit hit : safe(
                    () -> googlePlacesDiscoveryClient.discoverByCompanyNames(criteria),
                    "google-places-branches"
            )) {
                if (!nameLooksLikeRequestedBrand(hit.name(), criteria.companyNames())) {
                    log.debug("Skipping Places hit '{}' — not one of the requested brands", hit.name());
                    continue;
                }
                DiscoveredCompany company = toDiscoveredCompany(hit, criteria, providerName);
                unique.putIfAbsent(dedupeKey(company), company);
                notifyLiveCount(request, unique.size());
                if (unique.size() >= criteria.maxResults()) {
                    break;
                }
            }
            log.info(
                    "Named scrape Places branch expansion total={} for brands={}",
                    unique.size(),
                    criteria.companyNames()
            );
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
     * Looks up once per brand (not once per Places branch) and reuses the result.
     */
    private List<DiscoveredCompany> attachLeadership(List<DiscoveredCompany> companies) {
        Map<String, LeadershipPersonResponse> leadershipByBrand = new HashMap<>();
        List<DiscoveredCompany> enriched = new ArrayList<>(companies.size());
        for (DiscoveredCompany company : companies) {
            enriched.add(withLeadershipMetadata(company, leadershipByBrand));
        }
        return enriched;
    }

    private DiscoveredCompany withLeadershipMetadata(
            DiscoveredCompany company,
            Map<String, LeadershipPersonResponse> leadershipByBrand
    ) {
        if (company == null || company.name() == null || company.name().isBlank()) {
            return company;
        }
        try {
            String brandKey = NlRestaurantBrandSeed.normalizeKey(
                    NlRestaurantBrandSeed.canonicalBrandName(company.name())
            );
            if (brandKey.isBlank()) {
                brandKey = NlRestaurantBrandSeed.normalizeKey(company.name());
            }
            LeadershipPersonResponse lead = leadershipByBrand.computeIfAbsent(
                    brandKey,
                    ignored -> leadershipService.lookupOne(company.name())
            );
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
            metadata.put("brandName", lead.companyName());

            String titleLower = title.toLowerCase(Locale.ROOT);
            if (titleLower.contains("founder") || titleLower.contains("owner") || titleLower.contains("co-founder")) {
                metadata.put("founder", display);
            } else {
                metadata.put("ceo", display);
                metadata.putIfAbsent("founder", display);
            }
            log.info(
                    "Named scrape leadership {} (brand={}) -> {} via {}",
                    company.name(),
                    lead.companyName(),
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

    private int collectCompanies(
            List<WebSearchHit> hits,
            ResolvedDiscoveryCriteria criteria,
            String providerName,
            Map<String, DiscoveredCompany> unique
    ) {
        int rejected = 0;
        for (WebSearchHit hit : hits) {
            if (!isKeepableHit(hit)) {
                rejected++;
                continue;
            }
            if (!CategoryDiscoverySupport.matchesIndustry(hit, criteria)) {
                rejected++;
                continue;
            }
            DiscoveredCompany company = toDiscoveredCompany(hit, criteria, providerName);
            unique.putIfAbsent(dedupeKey(company), company);
            if (unique.size() >= criteria.maxResults()) {
                break;
            }
        }
        return rejected;
    }

    /**
     * Keep a real company website, or a maps/Places listing that at least has an address or phone.
     * Small-market AI/IT firms often have no website; dropping them made jobs look like the API key was missing.
     */
    private static boolean isKeepableHit(WebSearchHit hit) {
        if (WebsiteUrlSupport.isUsableCompanyWebsite(hit.website())) {
            return true;
        }
        boolean hasLocation = (hit.address() != null && !hit.address().isBlank())
                || (hit.phone() != null && !hit.phone().isBlank());
        if (!hasLocation) {
            return false;
        }
        String source = hit.providerSource() == null ? "" : hit.providerSource().toLowerCase(Locale.ROOT);
        return "google-places".equals(source)
                || "serpapi-maps".equals(source)
                || "apollo".equals(source);
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
        if (hit.address() != null && !hit.address().isBlank()) {
            metadata.put("address", hit.address().trim());
        }
        if (hit.phone() != null && !hit.phone().isBlank()) {
            metadata.put("phone", hit.phone().trim());
        }
        if (hit.placeId() != null && !hit.placeId().isBlank()) {
            metadata.put("placeId", hit.placeId().trim());
        }
        if ("google-places".equals(hit.providerSource()) || "serpapi-maps".equals(hit.providerSource())) {
            metadata.put("branchName", hit.name());
        }

        String idBasis;
        if (hit.placeId() != null && !hit.placeId().isBlank()) {
            idBasis = "place|" + hit.placeId().trim();
        } else if (hit.address() != null && !hit.address().isBlank()) {
            idBasis = normalizeWebsite(hit.website()) + "|" + hit.name() + "|" + hit.address().trim().toLowerCase(Locale.ROOT);
        } else {
            idBasis = normalizeWebsite(hit.website()) + "|" + hit.name();
        }
        String externalId = "web-" + UUID.nameUUIDFromBytes(
                idBasis.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        // Named scrapes: stamp a single category so multi-sheet Excel doesn't collapse onto sheet 1.
        List<String> categoryIds = criteria.categoryIds();
        if (criteria.hasCompanyNames() && categoryIds.size() > 1) {
            categoryIds = List.of(categoryIds.get(0));
        }

        String website = hit.website();
        if (criteria.hasCompanyNames()) {
            String seeded = officialWebsiteForRequestedBrand(hit.name(), criteria.companyNames());
            if (WebsiteUrlSupport.isUsableCompanyWebsite(seeded)) {
                if (website != null && !website.equalsIgnoreCase(seeded)) {
                    metadata.put("storePage", website);
                }
                website = seeded;
            } else {
                String homepage = WebsiteUrlSupport.brandHomepageUrl(website);
                if (WebsiteUrlSupport.isUsableCompanyWebsite(homepage) && !homepage.equals(website)) {
                    metadata.put("storePage", website);
                    website = homepage;
                }
            }
            metadata.put("namedScrape", true);
        }

        return new DiscoveredCompany(
                externalId,
                hit.name(),
                website,
                hit.countryCode(),
                cityName,
                cityId,
                categoryIds,
                hit.sourceUrl() == null || hit.sourceUrl().isBlank() ? website : hit.sourceUrl(),
                providerName,
                metadata
        );
    }

    private static String officialWebsiteForRequestedBrand(String placeName, List<String> brands) {
        if (brands == null) {
            return null;
        }
        for (String brand : brands) {
            if (!nameLooksLikeRequestedBrand(placeName, List.of(brand))) {
                continue;
            }
            String seeded = NlRestaurantBrandSeed.officialWebsite(
                    NlRestaurantBrandSeed.canonicalBrandName(brand)
            );
            if (WebsiteUrlSupport.isUsableCompanyWebsite(seeded)) {
                return seeded;
            }
        }
        return null;
    }

    private static boolean nameLooksLikeRequestedBrand(String placeName, List<String> brands) {
        if (placeName == null || placeName.isBlank() || brands == null || brands.isEmpty()) {
            return false;
        }
        String hay = placeName.toLowerCase(Locale.ROOT);
        for (String brand : brands) {
            if (brand == null || brand.isBlank()) {
                continue;
            }
            String needle = NlRestaurantBrandSeed.canonicalBrandName(brand).toLowerCase(Locale.ROOT);
            if (needle.length() >= 3 && hay.contains(needle)) {
                return true;
            }
            String compact = needle.replace("'", "").replace("’", "").replace(" ", "");
            String hayCompact = hay.replace("'", "").replace("’", "").replace(" ", "");
            if (compact.length() >= 4 && hayCompact.contains(compact)) {
                return true;
            }
        }
        return false;
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
        if (company.metadata() != null) {
            Object placeId = company.metadata().get("placeId");
            if (placeId != null && !placeId.toString().isBlank()) {
                return "place:" + placeId.toString().trim().toLowerCase(Locale.ROOT);
            }
            Object address = company.metadata().get("address");
            if (address != null && !address.toString().isBlank()) {
                String name = company.name() == null ? "" : company.name().trim().toLowerCase(Locale.ROOT);
                return "addr:" + name + "|" + address.toString().trim().toLowerCase(Locale.ROOT);
            }
        }
        String website = normalizeWebsite(company.website());
        if (!website.isBlank()) {
            return "web:" + website;
        }
        String name = company.name() == null ? "" : company.name().trim().toLowerCase(Locale.ROOT);
        String country = company.countryCode() == null ? "" : company.countryCode().trim().toUpperCase(Locale.ROOT);
        String city = company.cityName() == null ? "" : company.cityName().trim().toLowerCase(Locale.ROOT);
        return "name:" + name + "|" + country + "|" + city;
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
