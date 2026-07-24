/**
 * Picks and returns the contact scraper implementation to use.
 */
package com.datascraper.contact.factory;

import com.datascraper.contact.scraper.ContactScraper;
import com.datascraper.contact.scraper.impl.CompanyContactScraper;
import org.springframework.stereotype.Component;

@Component
public class ContactScraperFactory {

    private final CompanyContactScraper companyContactScraper;

    public ContactScraperFactory(CompanyContactScraper companyContactScraper) {
        this.companyContactScraper = companyContactScraper;
    }

    public ContactScraper companyContactScraper() {
        return companyContactScraper;
    }
}
