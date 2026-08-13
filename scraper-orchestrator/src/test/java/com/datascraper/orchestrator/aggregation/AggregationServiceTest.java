package com.datascraper.orchestrator.aggregation;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.provider.ProviderResult;
import com.datascraper.common.enums.ProviderType;
import com.datascraper.orchestrator.model.CompanyDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AggregationServiceTest {

    private AggregationService service;

    @BeforeEach
    void setUp() {
        service = new AggregationService();
    }

    @Test
    void mergesWebsiteAndContactResults() {
        DiscoveredCompany seed = new DiscoveredCompany(
                "co-1",
                "Acme",
                "https://acme.example.com",
                "DE",
                "Berlin",
                "city-1",
                List.of("cat-1"),
                "https://source.example.com",
                "catalog-seed",
                Map.of(
                        "address", "Friedrichstrasse 1, Berlin",
                        "phone", "+49 30 999",
                        "branchName", "Acme Berlin Mitte",
                        "placeId", "places/abc"
                )
        );

        List<ProviderResult> results = List.of(
                ProviderResult.success(
                        ProviderType.WEBSITE,
                        "website-remote",
                        "ok",
                        List.of(Map.of(
                                "section", "identity",
                                "field", "metaDescription",
                                "description", "Enterprise widgets"
                        )),
                        Map.of(),
                        0.8
                ),
                ProviderResult.success(
                        ProviderType.CONTACT,
                        "contact-remote",
                        "ok",
                        List.of(Map.of(
                                "section", "contact",
                                "field", "email",
                                "value", "sales@acme.example.com",
                                "sourceUrl", "https://acme.example.com/contact"
                        )),
                        Map.of(),
                        0.75
                )
        );

        CompanyDraft draft = service.aggregate(seed, results);

        assertThat(draft.getName()).isEqualTo("Acme");
        assertThat(draft.getWebsite()).isEqualTo("https://acme.example.com");
        assertThat(draft.getDescription()).isEqualTo("Enterprise widgets");
        assertThat(draft.getEmail()).isEqualTo("sales@acme.example.com");
        assertThat(draft.getContactPage()).isEqualTo("https://acme.example.com/contact");
        assertThat(draft.getAddress()).isEqualTo("Friedrichstrasse 1, Berlin");
        assertThat(draft.getPhone()).isEqualTo("+49 30 999");
        assertThat(draft.getRawAttributes().get("branchName")).isEqualTo("Acme Berlin Mitte");
        assertThat(draft.getRawAttributes().get("placeId")).isEqualTo("places/abc");
        assertThat(draft.getRawAttributes().get("branchId")).isEqualTo("abc");
        assertThat(draft.getSuccessfulProviderCount()).isEqualTo(2);
    }

    @Test
    void keepsSeedBranchManagerAndDoesNotCopyHomepageManagerOnNamedScrape() {
        DiscoveredCompany seed = new DiscoveredCompany(
                "co-2",
                "FEBO Rotterdam",
                "https://www.febo.nl",
                "NL",
                "Rotterdam",
                "nl-rotterdam",
                List.of("restaurant"),
                "https://maps.example",
                "google-places",
                Map.of(
                        "namedScrape", true,
                        "placeId", "places/xyz",
                        "branchManager", "Piet Jansen"
                )
        );

        List<ProviderResult> results = List.of(
                ProviderResult.success(
                        ProviderType.WEBSITE,
                        "website-remote",
                        "ok",
                        List.of(Map.of(
                                "section", "people",
                                "field", "branchManager",
                                "title", "Homepage Manager"
                        )),
                        Map.of(),
                        0.8
                )
        );

        CompanyDraft draft = service.aggregate(seed, results);

        assertThat(draft.getRawAttributes().get("branchManager")).isEqualTo("Piet Jansen");
    }
}
