/**
 * Holds the base URL for the Google scraper service.
 */
package com.datascraper.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scraper.services.google")
public record GoogleScraperProperties(String baseUrl) {
}
