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
    void mergesWebsiteContactGithubAndTechResults() {
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
                Map.of()
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
                ),
                ProviderResult.success(
                        ProviderType.GITHUB,
                        "github-remote",
                        "ok",
                        List.of(Map.of(
                                "section", "presence",
                                "field", "github-organization",
                                "profileUrl", "https://github.com/acme"
                        )),
                        Map.of(),
                        0.85
                ),
                ProviderResult.success(
                        ProviderType.TECHNOLOGY,
                        "tech-remote",
                        "ok",
                        List.of(Map.of(
                                "section", "technology",
                                "field", "framework",
                                "value", "Spring Boot"
                        )),
                        Map.of(),
                        0.7
                )
        );

        CompanyDraft draft = service.aggregate(seed, results);

        assertThat(draft.getName()).isEqualTo("Acme");
        assertThat(draft.getWebsite()).isEqualTo("https://acme.example.com");
        assertThat(draft.getDescription()).isEqualTo("Enterprise widgets");
        assertThat(draft.getEmail()).isEqualTo("sales@acme.example.com");
        assertThat(draft.getContactPage()).isEqualTo("https://acme.example.com/contact");
        assertThat(draft.getGithub()).isEqualTo("https://github.com/acme");
        assertThat(draft.getTechnologyStack()).contains("Spring Boot");
        assertThat(draft.getSuccessfulProviderCount()).isEqualTo(4);
    }
}
