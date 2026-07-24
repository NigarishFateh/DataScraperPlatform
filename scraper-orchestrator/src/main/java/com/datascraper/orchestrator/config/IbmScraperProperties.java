/**
 * Holds the base URL for the IBM scraper service.
 */
package com.datascraper.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scraper.services.ibm")
public record IbmScraperProperties(String baseUrl) {
}
