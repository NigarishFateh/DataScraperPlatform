/**
 * Social media link linked to an enriched company profile.
 */
package com.datascraper.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "company_socials")
public class CompanySocialEntity {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private CompanyProfileEntity profile;

    @Column(nullable = false, length = 64)
    private String platform;

    @Column(nullable = false, length = 512)
    private String url;

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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
