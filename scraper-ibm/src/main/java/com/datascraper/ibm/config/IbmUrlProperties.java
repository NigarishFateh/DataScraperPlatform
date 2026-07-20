package com.datascraper.ibm.config;

import com.datascraper.ibm.model.DataCategory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scraper.ibm.urls")
public record IbmUrlProperties(
        String jobs,
        String products,
        String services,
        String companyInfo,
        String contacts,
        String news
) {

    public String urlFor(DataCategory category) {
        return switch (category) {
            case JOBS -> jobs;
            case PRODUCTS -> products;
            case SERVICES -> services;
            case COMPANY_INFO -> companyInfo;
            case CONTACTS -> contacts;
            case NEWS -> news;
        };
    }

}
