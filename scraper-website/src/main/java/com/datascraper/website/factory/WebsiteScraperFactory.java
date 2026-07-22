package com.datascraper.website.factory;

import com.datascraper.website.scraper.WebsiteScraper;
import com.datascraper.website.scraper.impl.CompanyWebsiteScraper;
import org.springframework.stereotype.Component;

/**
 * Factory for website-local scraper strategies (extensible without controller changes).
 */
@Component
public class WebsiteScraperFactory {

    private final CompanyWebsiteScraper companyWebsiteScraper;

    public WebsiteScraperFactory(CompanyWebsiteScraper companyWebsiteScraper) {
        this.companyWebsiteScraper = companyWebsiteScraper;
    }

    public WebsiteScraper companyWebsiteScraper() {
        return companyWebsiteScraper;
    }
}
