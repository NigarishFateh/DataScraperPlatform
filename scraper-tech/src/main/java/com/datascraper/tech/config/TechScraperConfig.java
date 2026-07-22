package com.datascraper.tech.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TechScraperProperties.class)
public class TechScraperConfig {
}
