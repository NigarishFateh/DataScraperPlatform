/**
 * Location row linked to an enriched company profile.
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
@Table(name = "company_locations")
public class CompanyLocationEntity {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private CompanyProfileEntity profile;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(length = 255)
    private String state;

    @Column(length = 255)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String address;

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

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
