package com.datascraper.orchestrator.provider;

import com.datascraper.common.enums.ProviderType;
import com.datascraper.orchestrator.config.OrchestratorProperties;
import com.datascraper.orchestrator.scraper.impl.ContactRemoteScraper;
import com.datascraper.orchestrator.scraper.impl.WebsiteRemoteScraper;
import org.springframework.stereotype.Component;

@Component
public class WebsiteDataProvider extends ScraperDataProviderAdapter {

    public WebsiteDataProvider(WebsiteRemoteScraper scraper, OrchestratorProperties properties) {
        super(scraper, ProviderType.WEBSITE, "website-remote", properties.isProviderEnabled("website"));
    }
}

@Component
class ContactDataProvider extends ScraperDataProviderAdapter {

    public ContactDataProvider(ContactRemoteScraper scraper, OrchestratorProperties properties) {
        super(scraper, ProviderType.CONTACT, "contact-remote", properties.isProviderEnabled("contact"));
    }
}
