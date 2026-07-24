/**
 * Holds settings like user agent, timeout, and max items for tech scraping.
 */
package com.datascraper.tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tech.scraper")
public class TechScraperProperties {

    private String userAgent =
            "LeadIntelligenceBot/1.0 (+https://leadintelligence.local; respectful public scraper)";
    private int timeoutMs = 15000;
    private int maxItems = 30;

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
}
