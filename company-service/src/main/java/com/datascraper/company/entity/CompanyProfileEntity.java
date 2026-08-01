/**
 * Enriched company profile persisted per scraping job.
 */
package com.datascraper.company.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "company_profiles")
public class CompanyProfileEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "company_id", length = 64)
    private String companyId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String category;

    @Column(length = 255)
    private String industry;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(name = "country_name")
    private String countryName;

    @Column(length = 255)
    private String state;

    @Column(length = 255)
    private String city;

    @Column(length = 512)
    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String services;

    @Column(columnDefinition = "TEXT")
    private String products;

    @Column(length = 255)
    private String founder;

    @Column(length = 255)
    private String ceo;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(name = "employee_count", length = 64)
    private String employeeCount;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_page", length = 512)
    private String contactPage;

    @Column(name = "source_url", length = 512)
    private String sourceUrl;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "provider_name", length = 128)
    private String providerName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "scraped_at")
    private Instant scrapedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "normalized_key", nullable = false, length = 512)
    private String normalizedKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "company_profile_categories",
            joinColumns = @JoinColumn(name = "profile_id")
    )
    @Column(name = "category_id", length = 64)
    private List<String> categoryIds = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CompanyContactEntity> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CompanyLocationEntity> locations = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CompanyTechnologyEntity> technologies = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CompanySocialEntity> socials = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CompanySourceEntity> sources = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
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

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getProducts() {
        return products;
    }

    public void setProducts(String products) {
        this.products = products;
    }

    public String getFounder() {
        return founder;
    }

    public void setFounder(String founder) {
        this.founder = founder;
    }

    public String getCeo() {
        return ceo;
    }

    public void setCeo(String ceo) {
        this.ceo = ceo;
    }

    public Integer getFoundedYear() {
        return foundedYear;
    }

    public void setFoundedYear(Integer foundedYear) {
        this.foundedYear = foundedYear;
    }

    public String getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(String employeeCount) {
        this.employeeCount = employeeCount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactPage() {
        return contactPage;
    }

    public void setContactPage(String contactPage) {
        this.contactPage = contactPage;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getScrapedAt() {
        return scrapedAt;
    }

    public void setScrapedAt(Instant scrapedAt) {
        this.scrapedAt = scrapedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNormalizedKey() {
        return normalizedKey;
    }

    public void setNormalizedKey(String normalizedKey) {
        this.normalizedKey = normalizedKey;
    }

    public List<String> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<String> categoryIds) {
        this.categoryIds = categoryIds == null ? new ArrayList<>() : new ArrayList<>(categoryIds);
    }

    public List<CompanyContactEntity> getContacts() {
        return contacts;
    }

    public void setContacts(List<CompanyContactEntity> contacts) {
        this.contacts.clear();
        if (contacts != null) {
            contacts.forEach(this::addContact);
        }
    }

    public void addContact(CompanyContactEntity contact) {
        contact.setProfile(this);
        this.contacts.add(contact);
    }

    public List<CompanyLocationEntity> getLocations() {
        return locations;
    }

    public void setLocations(List<CompanyLocationEntity> locations) {
        this.locations.clear();
        if (locations != null) {
            locations.forEach(this::addLocation);
        }
    }

    public void addLocation(CompanyLocationEntity location) {
        location.setProfile(this);
        this.locations.add(location);
    }

    public List<CompanyTechnologyEntity> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<CompanyTechnologyEntity> technologies) {
        this.technologies.clear();
        if (technologies != null) {
            technologies.forEach(this::addTechnology);
        }
    }

    public void addTechnology(CompanyTechnologyEntity technology) {
        technology.setProfile(this);
        this.technologies.add(technology);
    }

    public List<CompanySocialEntity> getSocials() {
        return socials;
    }

    public void setSocials(List<CompanySocialEntity> socials) {
        this.socials.clear();
        if (socials != null) {
            socials.forEach(this::addSocial);
        }
    }

    public void addSocial(CompanySocialEntity social) {
        social.setProfile(this);
        this.socials.add(social);
    }

    public List<CompanySourceEntity> getSources() {
        return sources;
    }

    public void setSources(List<CompanySourceEntity> sources) {
        this.sources.clear();
        if (sources != null) {
            sources.forEach(this::addSource);
        }
    }

    public void addSource(CompanySourceEntity source) {
        source.setProfile(this);
        this.sources.add(source);
    }
}
