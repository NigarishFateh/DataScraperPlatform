/**
 * Raw provider source payload linked to an enriched company profile.
 */
package com.datascraper.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "company_sources")
public class CompanySourceEntity {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private CompanyProfileEntity profile;

    @Column(name = "provider_name", length = 128)
    private String providerName;

    @Column(name = "source_url", length = 512)
    private String sourceUrl;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "scraped_at")
    private Instant scrapedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CompanyProfileEntity getProfile() {
        return profile;
    }

    public void setProfile(CompanyProfileEntity profile) {
        this.profile = profile;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public Instant getScrapedAt() {
        return scrapedAt;
    }

    public void setScrapedAt(Instant scrapedAt) {
        this.scrapedAt = scrapedAt;
    }
}
