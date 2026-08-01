package com.datascraper.orchestrator.validation;

import com.datascraper.orchestrator.model.CompanyDraft;
import com.datascraper.orchestrator.model.ValidationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationServiceTest {

    private ValidationService service;

    @BeforeEach
    void setUp() {
        service = new ValidationService();
    }

    @Test
    void marksIncompleteWhenNameBlank() {
        CompanyDraft draft = new CompanyDraft();
        draft.setConfidenceScore(0.1);

        ValidationOutcome outcome = service.validate(draft);

        assertThat(outcome.incomplete()).isTrue();
        assertThat(outcome.valid()).isFalse();
        assertThat(outcome.warnings()).anyMatch(w -> w.contains("name"));
    }

    @Test
    void flagsInvalidEmailAndLowConfidence() {
        CompanyDraft draft = new CompanyDraft();
        draft.setName("Acme");
        draft.setEmail("not-an-email");
        draft.setConfidenceScore(0.1);

        ValidationOutcome outcome = service.validate(draft);

        assertThat(outcome.softFailure()).isTrue();
        assertThat(outcome.warnings()).anyMatch(w -> w.contains("email"));
        assertThat(outcome.warnings()).anyMatch(w -> w.contains("Confidence"));
    }

    @Test
    void acceptsValidRecord() {
        CompanyDraft draft = new CompanyDraft();
        draft.setName("Acme");
        draft.setWebsite("https://acme.example.com");
        draft.setCountryCode("DE");
        draft.setEmail("hello@acme.example.com");
        draft.setPhone("+493012345678");
        draft.setConfidenceScore(0.75);

        ValidationOutcome outcome = service.validate(draft);

        assertThat(outcome.valid()).isTrue();
        assertThat(outcome.softFailure()).isFalse();
    }
}
