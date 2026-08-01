/**
 * Turns on social scraper settings from configuration properties.
 */
package com.datascraper.social.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SocialScraperProperties.class)
public class SocialScraperConfig {
}
