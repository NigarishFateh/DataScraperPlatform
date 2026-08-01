package com.datascraper.export.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "export")
public class ExportProperties {

    private String storagePath = "./data/exports";
    private String appVersion = "1.0.0";
    private Queue queue = new Queue();

    @Getter
    @Setter
    public static class Queue {
        private boolean enabled = true;
        private long pollIntervalMs = 2000L;
    }
}
