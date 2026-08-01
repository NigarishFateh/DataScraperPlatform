/**
 * Contact details linked to an enriched company profile.
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
@Table(name = "company_contacts")
public class CompanyContactEntity {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private CompanyProfileEntity profile;

    @Column(length = 255)
    private String email;

    @Column(length = 64)
    private String phone;

    @Column(name = "contact_page", length = 512)
    private String contactPage;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getContactPage() {
        return contactPage;
    }

    public void setContactPage(String contactPage) {
        this.contactPage = contactPage;
    }
}
