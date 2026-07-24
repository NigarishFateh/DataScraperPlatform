/**
 * Holds the Google page URLs used for each scrape category.
 */
package com.datascraper.google.config;

import com.datascraper.google.model.DataCategory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scraper.google.urls")
public record GoogleUrlProperties(
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
