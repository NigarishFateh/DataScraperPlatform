/**
 * Holds settings like API URL, token, timeout, and max results for GitHub scraping.
 */
package com.datascraper.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github.scraper")
public class GitHubScraperProperties {

    private String userAgent =
            "LeadIntelligenceBot/1.0 (+https://leadintelligence.local; respectful public scraper)";
    private int timeoutMs = 10000;
    private int maxResults = 5;
    private String apiBaseUrl = "https://api.github.com";
    private String token = "";

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

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
