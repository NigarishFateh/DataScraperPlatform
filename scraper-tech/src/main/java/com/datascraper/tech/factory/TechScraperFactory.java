/**
 * Picks and returns the tech scraper implementation to use.
 */
package com.datascraper.tech.factory;

import com.datascraper.tech.scraper.TechScraper;
import com.datascraper.tech.scraper.impl.CompanyTechScraper;
import org.springframework.stereotype.Component;

@Component
public class TechScraperFactory {

    private final CompanyTechScraper companyTechScraper;

    public TechScraperFactory(CompanyTechScraper companyTechScraper) {
        this.companyTechScraper = companyTechScraper;
    }

    public TechScraper companyTechScraper() {
        return companyTechScraper;
    }
}
