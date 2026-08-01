package com.datascraper.export;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ExportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExportServiceApplication.class, args);
    }
}
