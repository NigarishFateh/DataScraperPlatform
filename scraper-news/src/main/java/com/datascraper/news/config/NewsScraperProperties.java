package com.datascraper.news.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "news.scraper")
public class NewsScraperProperties {

    private String userAgent =
            "LeadIntelligenceBot/1.0 (+https://leadintelligence.local; respectful public scraper)";
    private int timeoutMs = 15000;
    private int maxItems = 10;
    private String rssBaseUrl = "https://news.google.com/rss/search";

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    public String getRssBaseUrl() {
        return rssBaseUrl;
    }

    public void setRssBaseUrl(String rssBaseUrl) {
        this.rssBaseUrl = rssBaseUrl;
    }
}
