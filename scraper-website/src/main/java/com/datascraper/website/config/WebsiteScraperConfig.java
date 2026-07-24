/**
 * Turns on website scraper settings from configuration properties.
 */
package com.datascraper.website.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WebsiteScraperProperties.class)
public class WebsiteScraperConfig {
}
