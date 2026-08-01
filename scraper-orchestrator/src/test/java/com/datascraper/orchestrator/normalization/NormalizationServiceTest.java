package com.datascraper.orchestrator.normalization;

import com.datascraper.orchestrator.model.CompanyDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizationServiceTest {

    private NormalizationService service;

    @BeforeEach
    void setUp() {
        service = new NormalizationService();
    }

    @Test
    void normalizesCoreFieldsAndDuplicateKey() {
        CompanyDraft draft = new CompanyDraft();
        draft.setName("  Acme   Corp  ");
        draft.setCountryCode("de");
        draft.setCity("berlin");
        draft.setWebsite("Acme.example.com/");
        draft.setEmail("Contact@Acme.Example.com");
        draft.setPhone("+49 (30) 123-4567");
        draft.setLinkedIn("linkedin.com/company/acme");
        draft.getTechnologyStack().addAll(List.of("spring boot", "postgresql"));
        draft.setSuccessfulProviderCount(3);

        service.normalize(draft);

        assertThat(draft.getName()).isEqualTo("Acme Corp");
        assertThat(draft.getCountryCode()).isEqualTo("DE");
        assertThat(draft.getCity()).isEqualTo("Berlin");
        assertThat(draft.getWebsite()).isEqualTo("https://acme.example.com");
        assertThat(draft.getEmail()).isEqualTo("contact@acme.example.com");
        assertThat(draft.getPhone()).isEqualTo("+49301234567");
        assertThat(draft.getLinkedIn()).isEqualTo("https://linkedin.com/company/acme");
        assertThat(draft.getTechnologyStack()).containsExactly("Spring Boot", "PostgreSQL");
        assertThat(draft.getDuplicateKey()).isEqualTo("website:https://acme.example.com");
        assertThat(draft.getConfidenceScore()).isGreaterThan(0.3);
    }

    @Test
    void buildsNameCountryDuplicateKeyWhenWebsiteMissing() {
        CompanyDraft draft = new CompanyDraft();
        draft.setName("Beta GmbH");
        draft.setCountryCode("CH");
        draft.setSuccessfulProviderCount(1);

        service.normalize(draft);

        assertThat(draft.getDuplicateKey()).isEqualTo("name-country:beta gmbh|CH");
    }
}
