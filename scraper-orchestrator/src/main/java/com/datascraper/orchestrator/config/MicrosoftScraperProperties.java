/**
 * Holds the base URL for the Microsoft scraper service.
 */
package com.datascraper.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scraper.services.microsoft")
public record MicrosoftScraperProperties(String baseUrl) {
}
