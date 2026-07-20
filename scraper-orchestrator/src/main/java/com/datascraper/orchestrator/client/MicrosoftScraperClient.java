package com.datascraper.orchestrator.client;

import com.datascraper.orchestrator.config.MicrosoftScraperProperties;
import com.datascraper.orchestrator.config.ScraperResilienceProperties;
import com.datascraper.orchestrator.model.ScraperSource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class MicrosoftScraperClient extends AbstractScraperClient {

    public MicrosoftScraperClient(
            WebClient webClient,
            MicrosoftScraperProperties microsoftScraperProperties,
            ScraperResilienceProperties resilienceProperties) {
        super(webClient, microsoftScraperProperties.baseUrl(), ScraperSource.MICROSOFT, resilienceProperties);
    }

}
