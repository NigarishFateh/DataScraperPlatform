/**
 * Starts the scraper-orchestrator Spring Boot service.
 */
package com.datascraper.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScraperOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScraperOrchestratorApplication.class, args);
    }

}
