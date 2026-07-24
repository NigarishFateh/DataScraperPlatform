/**
 * Turns on news scraper settings from configuration properties.
 */
package com.datascraper.news.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NewsScraperProperties.class)
public class NewsScraperConfig {
}
