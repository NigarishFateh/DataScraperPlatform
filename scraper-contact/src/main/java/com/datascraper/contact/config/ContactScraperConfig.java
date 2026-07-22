package com.datascraper.contact.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ContactScraperProperties.class)
public class ContactScraperConfig {
}
