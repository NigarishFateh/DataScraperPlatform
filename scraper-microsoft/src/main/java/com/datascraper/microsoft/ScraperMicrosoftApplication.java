/**
 * Starts the Microsoft scraper Spring Boot service.
 */
package com.datascraper.microsoft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScraperMicrosoftApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScraperMicrosoftApplication.class, args);
    }

}
