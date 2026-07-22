package com.datascraper.news.factory;

import com.datascraper.news.scraper.NewsScraper;
import com.datascraper.news.scraper.impl.CompanyNewsScraper;
import org.springframework.stereotype.Component;

@Component
public class NewsScraperFactory {

    private final CompanyNewsScraper companyNewsScraper;

    public NewsScraperFactory(CompanyNewsScraper companyNewsScraper) {
        this.companyNewsScraper = companyNewsScraper;
    }

    public NewsScraper companyNewsScraper() {
        return companyNewsScraper;
    }
}
