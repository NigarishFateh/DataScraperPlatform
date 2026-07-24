/**
 * Starts the news scraper Spring Boot service.
 */
package com.datascraper.news;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NewsScraperApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsScraperApplication.class, args);
    }
}
