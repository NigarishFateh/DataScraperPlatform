package com.datascraper.discovery.service;

import com.datascraper.discovery.client.ApolloPeopleLeadershipClient;
import com.datascraper.discovery.client.FmpExecutivesClient;
import com.datascraper.discovery.client.OpenLeadershipClient;
import com.datascraper.discovery.client.SerpApiLeadershipClient;
import com.datascraper.discovery.dto.LeadershipLookupResponse;
import com.datascraper.discovery.dto.LeadershipPersonResponse;
import com.datascraper.discovery.support.NlRestaurantBrandSeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Leadership lookup for brand / company names (FMP → curated → Apollo → open → SerpAPI).
 * Used by the isolated leadership API and by custom (named) scrape discovery.
 */
@Service
public class NlRestaurantLeadershipService {

    private static final Logger log = LoggerFactory.getLogger(NlRestaurantLeadershipService.class);

    private final FmpExecutivesClient fmpExecutivesClient;
    private final ApolloPeopleLeadershipClient apolloPeopleLeadershipClient;
    private final OpenLeadershipClient openLeadershipClient;
    private final SerpApiLeadershipClient serpApiLeadershipClient;

    public NlRestaurantLeadershipService(
            FmpExecutivesClient fmpExecutivesClient,
            ApolloPeopleLeadershipClient apolloPeopleLeadershipClient,
            OpenLeadershipClient openLeadershipClient,
            SerpApiLeadershipClient serpApiLeadershipClient
    ) {
        this.fmpExecutivesClient = fmpExecutivesClient;
        this.apolloPeopleLeadershipClient = apolloPeopleLeadershipClient;
        this.openLeadershipClient = openLeadershipClient;
        this.serpApiLeadershipClient = serpApiLeadershipClient;
    }

    public LeadershipLookupResponse lookup(List<String> requestedNames) {
        List<String> brands = resolveBrandList(requestedNames);
        List<LeadershipPersonResponse> results = new ArrayList<>();
        int found = 0;

        for (String brand : brands) {
            LeadershipPersonResponse row = lookupOne(brand);
            results.add(row);
            if (row.found()) {
                found++;
            }
        }

        String notes = "Sources tried in order: FMP (public tickers), curated seed, Apollo People (if plan allows), "
                + "Wikipedia/Wikidata/DuckDuckGo, then SerpAPI Google.";

        return new LeadershipLookupResponse(brands.size(), found, notes, results);
    }

    public LeadershipPersonResponse lookupOne(String brand) {
        String canonical = NlRestaurantBrandSeed.canonicalBrandName(brand);
        String website = NlRestaurantBrandSeed.officialWebsite(canonical);
        String domain = NlRestaurantBrandSeed.domain(canonical);
        String ticker = NlRestaurantBrandSeed.stockTicker(canonical);

        // 1) FMP executives for publicly traded brands (best CEO/comp data)
        if (ticker != null && fmpExecutivesClient.isConfigured()) {
            Optional<FmpExecutivesClient.ExecutiveLead> fmp =
                    fmpExecutivesClient.findLeadership(ticker, canonical);
            if (fmp.isPresent()) {
                FmpExecutivesClient.ExecutiveLead lead = fmp.get();
                log.info("Leadership {} -> {} ({}) via fmp:{}",
                        canonical, lead.name(), lead.title(), lead.ticker());
                return hit(
                        canonical,
                        website,
                        lead.name(),
                        lead.title(),
                        lead.source(),
                        lead.ticker(),
                        lead.compensation()
                );
            }
        }

        // 2) Curated seed early for known brands (fast path; avoids Apollo 403 / SerpAPI 429 waits)
        NlRestaurantBrandSeed.KnownLeader known = NlRestaurantBrandSeed.knownLeader(canonical);
        if (known != null
                && known.name() != null
                && !known.name().toLowerCase(java.util.Locale.ROOT).contains("leadership")) {
            log.info("Leadership {} -> {} ({}) via curated-seed", canonical, known.name(), known.title());
            return hit(canonical, website, known.name(), known.title(), "curated-seed", ticker, null);
        }

        // 3) Apollo People (optional — Free plans usually 403)
        if (domain != null) {
            Optional<ApolloPeopleLeadershipClient.Lead> apollo =
                    apolloPeopleLeadershipClient.findLeadership(domain, "Netherlands");
            if (apollo.isPresent()) {
                ApolloPeopleLeadershipClient.Lead lead = apollo.get();
                log.info("Leadership {} -> {} ({}) via {}", canonical, lead.name(), lead.title(), lead.source());
                return hit(canonical, website, lead.name(), lead.title(), lead.source());
            }
        }

        // 4) Free open sources (unknown brands only)
        Optional<OpenLeadershipClient.Lead> open = openLeadershipClient.findLeadership(canonical, "Netherlands");
        if (open.isPresent() && open.get().score() >= 70) {
            OpenLeadershipClient.Lead lead = open.get();
            log.info("Leadership {} -> {} ({}) via {}", canonical, lead.name(), lead.title(), lead.source());
            return hit(canonical, website, lead.name(), lead.title(), lead.source());
        }

        // 5) SerpAPI Google (skipped after first 429/401/403 for this process)
        Optional<OpenLeadershipClient.Lead> serp = serpApiLeadershipClient.findLeadership(canonical);
        if (serp.isPresent()) {
            OpenLeadershipClient.Lead lead = serp.get();
            log.info("Leadership {} -> {} ({}) via {}", canonical, lead.name(), lead.title(), lead.source());
            return hit(canonical, website, lead.name(), lead.title(), lead.source());
        }

        // Keep weaker open hit if SerpAPI empty
        if (open.isPresent()) {
            OpenLeadershipClient.Lead lead = open.get();
            return hit(canonical, website, lead.name(), lead.title(), lead.source());
        }

        log.info("Leadership {} -> not found", canonical);
        return new LeadershipPersonResponse(canonical, website, null, null, "none", false, ticker, null);
    }

    private static LeadershipPersonResponse hit(
            String company,
            String website,
            String name,
            String title,
            String source
    ) {
        return hit(company, website, name, title, source, null, null);
    }

    private static LeadershipPersonResponse hit(
            String company,
            String website,
            String name,
            String title,
            String source,
            String ticker,
            Long compensation
    ) {
        return new LeadershipPersonResponse(
                company, website, name, title, source, true, ticker, compensation
        );
    }

    private static List<String> resolveBrandList(List<String> requestedNames) {
        if (requestedNames == null || requestedNames.isEmpty()) {
            return NlRestaurantBrandSeed.BRANDS;
        }
        Set<String> requestedKeys = new LinkedHashSet<>();
        for (String raw : requestedNames) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            for (String part : raw.split(",")) {
                if (part != null && !part.isBlank()) {
                    requestedKeys.add(NlRestaurantBrandSeed.normalizeKey(part.trim()));
                }
            }
        }
        if (requestedKeys.isEmpty()) {
            return NlRestaurantBrandSeed.BRANDS;
        }

        List<String> ordered = new ArrayList<>();
        for (String brand : NlRestaurantBrandSeed.BRANDS) {
            if (requestedKeys.remove(NlRestaurantBrandSeed.normalizeKey(brand))) {
                ordered.add(brand);
            }
        }
        // Aliases already mapped by canonicalBrandName before normalize; leftovers are custom names.
        for (String leftover : requestedKeys) {
            ordered.add(NlRestaurantBrandSeed.canonicalBrandName(leftover));
        }
        return ordered;
    }
}
