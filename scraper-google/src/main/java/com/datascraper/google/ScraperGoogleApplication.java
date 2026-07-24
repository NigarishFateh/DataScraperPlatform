/**
 * Starts the Google scraper Spring Boot service.
 */
package com.datascraper.google;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScraperGoogleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScraperGoogleApplication.class, args);
    }

}
