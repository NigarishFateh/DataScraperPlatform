/**
 * Turns on Microsoft URL settings from configuration properties.
 */
package com.datascraper.microsoft.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MicrosoftUrlProperties.class)
public class ScraperConfig {
}
