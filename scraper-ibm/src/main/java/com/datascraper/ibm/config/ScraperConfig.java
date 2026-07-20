package com.datascraper.ibm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IbmUrlProperties.class)
public class ScraperConfig {
}
