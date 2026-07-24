/**
 * Starts the tech stack scraper Spring Boot service.
 */
package com.datascraper.tech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TechScraperApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechScraperApplication.class, args);
    }
}
