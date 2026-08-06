package com.datascraper.common.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyEmailSupportTest {

    @Test
    void cleansMailtoAndRejectsNoreply() {
        assertEquals("info@acme.io", CompanyEmailSupport.clean("mailto:Info@Acme.io?subject=Hi"));
        assertNull(CompanyEmailSupport.clean("noreply@acme.io"));
        assertNull(CompanyEmailSupport.clean("user@example.com"));
    }

    @Test
    void extractsObfuscatedEmails() {
        List<String> emails = CompanyEmailSupport.extractFromText("Reach us at sales [at] acme.io or hello(at)acme(dot)io");
        assertTrue(emails.contains("sales@acme.io"));
        assertTrue(emails.contains("hello@acme.io"));
    }

    @Test
    void ranksCompanyDomainRoleEmailHighest() {
        List<String> ranked = CompanyEmailSupport.rank(
                List.of("person@gmail.com", "noreply@acme.io", "info@acme.io"),
                "https://www.acme.io"
        );
        assertEquals(List.of("info@acme.io", "person@gmail.com"), ranked);
        assertFalse(ranked.contains("noreply@acme.io"));
    }

    @Test
    void preferUpgradesToBetterEmail() {
        String chosen = CompanyEmailSupport.prefer(
                "random@gmail.com",
                "contact@acme.io",
                "https://acme.io"
        );
        assertEquals("contact@acme.io", chosen);
    }
}
