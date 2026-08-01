package com.datascraper.discovery.service;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyDeduplicationServiceTest {

    private CompanyDeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        deduplicationService = new CompanyDeduplicationService();
    }

    @Test
    void deduplicatesByNormalizedWebsite() {
        DiscoveredCompany first = company("1", "Acme GmbH", "https://www.acme.example/", "DE");
        DiscoveredCompany duplicate = company("2", "Acme", "http://acme.example", "DE");

        List<DiscoveredCompany> unique = deduplicationService.deduplicate(List.of(first, duplicate));

        assertThat(unique).hasSize(1);
        assertThat(unique.get(0).externalId()).isEqualTo("1");
    }

    @Test
    void deduplicatesByNameAndCountryWhenWebsiteMissing() {
        DiscoveredCompany first = company("1", "Beta AG", null, "CH");
        DiscoveredCompany duplicate = company("2", "beta ag", "", "ch");

        List<DiscoveredCompany> unique = deduplicationService.deduplicate(List.of(first, duplicate));

        assertThat(unique).hasSize(1);
    }

    @Test
    void keepsDistinctCompaniesWithDifferentCountries() {
        DiscoveredCompany de = company("1", "Global Corp", null, "DE");
        DiscoveredCompany us = company("2", "Global Corp", null, "US");

        List<DiscoveredCompany> unique = deduplicationService.deduplicate(List.of(de, us));

        assertThat(unique).hasSize(2);
    }

    private static DiscoveredCompany company(
            String id,
            String name,
            String website,
            String country
    ) {
        return new DiscoveredCompany(
                id,
                name,
                website,
                country,
                null,
                null,
                List.of("cat-1"),
                website,
                "test-provider",
                Map.of()
        );
    }
}
