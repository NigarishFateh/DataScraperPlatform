/**
 * Domain model for a company catalog record.
 */
package com.datascraper.company.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Company master record — catalog data, not scraped intelligence.
 */
public class Company {

    private String id;
    private String name;
    private String website;
    private String industry;
    private String cityId;
    private String countryCode;
    private List<String> categoryIds = new ArrayList<>();

    public Company() {
    }

    public Company(
            String id,
            String name,
            String website,
            String industry,
            String cityId,
            String countryCode,
            List<String> categoryIds
    ) {
        this.id = id;
        this.name = name;
        this.website = website;
        this.industry = industry;
        this.cityId = cityId;
        this.countryCode = countryCode;
        this.categoryIds = new ArrayList<>(categoryIds);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public List<String> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<String> categoryIds) {
        this.categoryIds = new ArrayList<>(categoryIds);
    }
}
