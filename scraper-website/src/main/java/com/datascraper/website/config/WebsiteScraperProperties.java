/**
 * Holds settings like user agent, timeout, and robots.txt for the website scraper.
 */
package com.datascraper.website.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "website.scraper")
public class WebsiteScraperProperties {

    private String userAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";
    private int timeoutMs = 8000;
    private int maxItems = 40;
    private boolean respectRobotsTxt = true;

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

    public boolean isRespectRobotsTxt() {
        return respectRobotsTxt;
    }

    public void setRespectRobotsTxt(boolean respectRobotsTxt) {
        this.respectRobotsTxt = respectRobotsTxt;
    }
}
