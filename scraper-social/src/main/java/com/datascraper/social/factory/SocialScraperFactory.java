/**
 * Picks and returns the social scraper implementation to use.
 */
package com.datascraper.social.factory;

import com.datascraper.social.scraper.SocialScraper;
import com.datascraper.social.scraper.impl.CompanySocialScraper;
import org.springframework.stereotype.Component;

@Component
public class SocialScraperFactory {

    private final CompanySocialScraper companySocialScraper;

    public SocialScraperFactory(CompanySocialScraper companySocialScraper) {
        this.companySocialScraper = companySocialScraper;
    }

    public SocialScraper companySocialScraper() {
        return companySocialScraper;
    }
}
