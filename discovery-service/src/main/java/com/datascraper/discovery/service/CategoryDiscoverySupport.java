package com.datascraper.discovery.service;

import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import com.datascraper.discovery.dto.WebSearchHit;

import java.util.Locale;
import java.util.Set;

/**
 * Detects tech-oriented categories and filters obviously wrong-industry hits.
 * Web search hits are already scoped by industry+city queries, so we do not require
 * the company display name to contain the category phrase (e.g. "Orient" for advertising).
 */
final class CategoryDiscoverySupport {

    private static final Set<String> TECH_CATEGORY_IDS = Set.of(
            "ai", "ml", "software", "software-dev", "web-dev", "mobile", "mobile-dev",
            "saas", "paas", "iaas", "cloud", "devops", "sre", "cyber", "cybersecurity",
            "infosec", "data-eng", "data-science", "big-data", "analytics", "bi-software",
            "erp", "crm", "api", "blockchain", "web3", "iot", "robotics", "ar-vr",
            "gaming", "game-dev", "it", "it-services", "it-support", "msp", "outsourcing",
            "qa-testing", "ui-ux", "digital-xform", "automation", "rpa", "low-code",
            "embedded", "semiconductor", "fintech", "healthtech", "edtech", "legaltech",
            "regtech", "govtech", "martech", "adtech", "hrtech", "proptech", "contech",
            "cleantech", "greentech", "foodtech", "retailtech", "logistics-tech",
            "mobility-tech", "climate-tech", "space-tech", "defense-tech", "medtech",
            "health-it", "emr", "telehealth-platform"
    );

    private static final Set<String> TECH_NOISE_TOKENS = Set.of(
            "software house",
            "software development",
            "devops",
            "github.com",
            "stackoverflow",
            "programming bootcamp",
            "it company",
            "saas platform"
    );

    private CategoryDiscoverySupport() {
    }

    static boolean isTechOriented(ResolvedDiscoveryCriteria criteria) {
        if (criteria.categoryIds() != null) {
            for (String id : criteria.categoryIds()) {
                if (id != null && TECH_CATEGORY_IDS.contains(id.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        if (criteria.searchKeywords() != null) {
            for (String keyword : criteria.searchKeywords()) {
                if (keyword == null) {
                    continue;
                }
                String lower = keyword.toLowerCase(Locale.ROOT);
                if (lower.contains("software development")
                        || lower.contains("artificial intelligence")
                        || lower.contains("machine learning")
                        || lower.equals("saas")
                        || lower.contains("devops")) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean matchesIndustry(WebSearchHit hit, ResolvedDiscoveryCriteria criteria) {
        if (hit == null) {
            return false;
        }
        String haystack = ((hit.name() == null ? "" : hit.name()) + " "
                + (hit.website() == null ? "" : hit.website()) + " "
                + (hit.sourceUrl() == null ? "" : hit.sourceUrl())).toLowerCase(Locale.ROOT);

        boolean tech = isTechOriented(criteria);
        String source = hit.providerSource() == null ? "" : hit.providerSource().toLowerCase(Locale.ROOT);

        // DuckDuckGo / Wikidata queries already include industry + city. Accept unless clearly wrong.
        if ("duckduckgo".equals(source) || "wikidata".equals(source)) {
            if (!tech && looksLikePureTechNoise(haystack) && !containsAnyKeywordToken(haystack, criteria)) {
                return false;
            }
            return true;
        }

        if ("openstreetmap".equals(source)
                || "google-places".equals(source)
                || "serpapi-maps".equals(source)) {
            if (!tech && looksLikePureTechNoise(haystack) && !containsAnyKeywordToken(haystack, criteria)) {
                return false;
            }
            // These providers already search by industry + city + country.
            return true;
        }

        // GitHub and other sources: require an industry signal in the hit text.
        return containsAnyKeywordToken(haystack, criteria);
    }

    private static boolean containsAnyKeywordToken(String haystack, ResolvedDiscoveryCriteria criteria) {
        if (criteria.searchKeywords() == null || criteria.searchKeywords().isEmpty()) {
            return true;
        }
        for (String keyword : criteria.searchKeywords()) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            String lowerKeyword = keyword.toLowerCase(Locale.ROOT).trim();
            if (lowerKeyword.length() >= 3 && haystack.contains(lowerKeyword)) {
                return true;
            }
            for (String token : lowerKeyword.split("[\\s/,&-]+")) {
                if (token.length() >= 4 && haystack.contains(token)) {
                    return true;
                }
            }
        }
        if (criteria.categoryNames() != null) {
            for (String name : criteria.categoryNames()) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                for (String token : name.toLowerCase(Locale.ROOT).split("[\\s/,&-]+")) {
                    if (token.length() >= 4 && haystack.contains(token)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean looksLikePureTechNoise(String haystack) {
        for (String noise : TECH_NOISE_TOKENS) {
            if (haystack.contains(noise)) {
                return true;
            }
        }
        return false;
    }
}
