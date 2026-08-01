package com.datascraper.orchestrator.provider;

import com.datascraper.common.enums.ProviderType;
import com.datascraper.orchestrator.config.OrchestratorProperties;
import com.datascraper.orchestrator.scraper.impl.ContactRemoteScraper;
import com.datascraper.orchestrator.scraper.impl.GitHubRemoteScraper;
import com.datascraper.orchestrator.scraper.impl.NewsRemoteScraper;
import com.datascraper.orchestrator.scraper.impl.SocialRemoteScraper;
import com.datascraper.orchestrator.scraper.impl.TechStackRemoteScraper;
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

@Component
class GitHubDataProvider extends ScraperDataProviderAdapter {

    public GitHubDataProvider(GitHubRemoteScraper scraper, OrchestratorProperties properties) {
        super(scraper, ProviderType.GITHUB, "github-remote", properties.isProviderEnabled("github"));
    }
}

@Component
class TechStackDataProvider extends ScraperDataProviderAdapter {

    public TechStackDataProvider(TechStackRemoteScraper scraper, OrchestratorProperties properties) {
        super(scraper, ProviderType.TECHNOLOGY, "tech-remote", properties.isProviderEnabled("technology"));
    }
}

@Component
class NewsDataProvider extends ScraperDataProviderAdapter {

    public NewsDataProvider(NewsRemoteScraper scraper, OrchestratorProperties properties) {
        super(scraper, ProviderType.NEWS, "news-remote", properties.isProviderEnabled("news"));
    }
}

@Component
class SocialDataProvider extends ScraperDataProviderAdapter {

    public SocialDataProvider(SocialRemoteScraper scraper, OrchestratorProperties properties) {
        super(scraper, ProviderType.SOCIAL, "social-remote", properties.isProviderEnabled("social"));
    }
}
