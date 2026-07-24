/**
 * Turns on Google URL settings from configuration properties.
 */
package com.datascraper.google.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GoogleUrlProperties.class)
public class ScraperConfig {
}
