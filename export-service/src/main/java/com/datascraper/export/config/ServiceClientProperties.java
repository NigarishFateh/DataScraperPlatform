package com.datascraper.export.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "services")
public class ServiceClientProperties {

    private ServiceEndpoint company = new ServiceEndpoint("http://localhost:8083");
    private ServiceEndpoint job = new ServiceEndpoint("http://localhost:8086");

    @Getter
    @Setter
    public static class ServiceEndpoint {
        private String baseUrl;

        public ServiceEndpoint() {
        }

        public ServiceEndpoint(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
