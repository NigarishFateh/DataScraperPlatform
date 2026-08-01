/**
 * Integration tests for enriched profile upsert and by-job pagination.
 */
package com.datascraper.company.service;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.company.dto.EnrichedCompanyUpsertResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CompanyProfileEnrichedIntegrationTest {

    @Autowired
    private CompanyProfileService companyProfileService;

    @Test
    void upsertsByNormalizedKeyWithinJobAndPaginatesForExport() {
        UUID jobId = UUID.randomUUID();
        Instant scrapedAt = Instant.parse("2026-01-15T10:30:00Z");

        EnrichedCompany initial = sampleCompany(
                null,
                "Acme Global Inc",
                "https://www.acme-global.com/about",
                "US",
                "United States",
                "California",
                "San Francisco",
                "contact@acme-global.com",
                "+1 415 555 0100",
                List.of("Java", "PostgreSQL"),
                scrapedAt,
                0.88,
                Map.of("source", "website")
        );

        EnrichedCompanyUpsertResponse created = companyProfileService.upsertEnriched(jobId, initial);
        assertThat(created.id()).isNotBlank();

        EnrichedCompany updatedPayload = sampleCompany(
                created.id(),
                "Acme Global Inc",
                "https://acme-global.com",
                "US",
                "United States",
                "California",
                "San Francisco",
                "sales@acme-global.com",
                "+1 415 555 0199",
                List.of("Java", "PostgreSQL", "Kafka"),
                scrapedAt,
                0.95,
                Map.of("source", "website", "pass", "second")
        );

        EnrichedCompanyUpsertResponse updated = companyProfileService.upsertEnriched(jobId, updatedPayload);
        assertThat(updated.id()).isEqualTo(created.id());

        EnrichedCompany stored = companyProfileService.getProfileById(created.id());
        assertThat(stored.email()).isEqualTo("sales@acme-global.com");
        assertThat(stored.technologyStack()).containsExactly("Java", "PostgreSQL", "Kafka");
        assertThat(stored.confidenceScore()).isEqualTo(0.95);

        UUID otherJobId = UUID.randomUUID();
        EnrichedCompanyUpsertResponse otherJob = companyProfileService.upsertEnriched(
                otherJobId,
                sampleCompany(
                        null,
                        "Acme Global Inc",
                        "https://acme-global.com",
                        "US",
                        "United States",
                        "California",
                        "San Francisco",
                        null,
                        null,
                        List.of(),
                        scrapedAt,
                        0.5,
                        Map.of()
                )
        );
        assertThat(otherJob.id()).isNotEqualTo(created.id());

        EnrichedCompany india = sampleCompany(
                null,
                "Bengaluru Tech Pvt Ltd",
                "https://bengalurutech.in",
                "IN",
                "India",
                "Karnataka",
                "Bengaluru",
                "hello@bengalurutech.in",
                "+91 80 5555 0100",
                List.of("Spring Boot"),
                scrapedAt,
                0.81,
                Map.of("region", "APAC")
        );
        companyProfileService.upsertEnriched(jobId, india);

        PageResponse<EnrichedCompany> page0 = companyProfileService.findByJob(jobId, 0, 1);
        PageResponse<EnrichedCompany> page1 = companyProfileService.findByJob(jobId, 1, 1);

        assertThat(page0.total()).isEqualTo(2);
        assertThat(page0.items()).hasSize(1);
        assertThat(page0.hasMore()).isTrue();
        assertThat(page1.items()).hasSize(1);
        assertThat(page0.items().get(0).id()).isNotEqualTo(page1.items().get(0).id());
    }

    private EnrichedCompany sampleCompany(
            String id,
            String name,
            String website,
            String countryCode,
            String countryName,
            String state,
            String city,
            String email,
            String phone,
            List<String> stack,
            Instant scrapedAt,
            double confidence,
            Map<String, Object> rawAttributes
    ) {
        return new EnrichedCompany(
                id,
                name,
                "Software",
                "Information Technology",
                countryCode,
                countryName,
                state,
                city,
                website,
                email,
                phone,
                null,
                null,
                "Enterprise platform provider",
                "Consulting",
                "Platform",
                stack,
                "https://linkedin.com/company/acme-global",
                null,
                null,
                null,
                null,
                null,
                2018,
                "51-200",
                "1 Market Street",
                website + "/contact",
                website,
                scrapedAt,
                confidence,
                "website-scraper",
                null,
                List.of("cat-software"),
                rawAttributes
        );
    }
}
