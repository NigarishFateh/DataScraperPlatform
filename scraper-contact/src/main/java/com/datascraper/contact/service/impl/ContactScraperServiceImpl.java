/**
 * Runs a contact scrape by calling the contact scraper factory.
 */
package com.datascraper.contact.service.impl;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.contact.factory.ContactScraperFactory;
import com.datascraper.contact.service.ContactScraperService;
import org.springframework.stereotype.Service;

@Service
public class ContactScraperServiceImpl implements ContactScraperService {

    private final ContactScraperFactory scraperFactory;

    public ContactScraperServiceImpl(ContactScraperFactory scraperFactory) {
        this.scraperFactory = scraperFactory;
    }

    @Override
    public ScraperResult scrape(ScraperContext context) {
        return scraperFactory.companyContactScraper().scrape(context);
    }
}
