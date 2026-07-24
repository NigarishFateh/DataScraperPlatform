/**
 * HTTP client that talks to the Google scraper service.
 */
package com.datascraper.orchestrator.client;

import com.datascraper.orchestrator.config.GoogleScraperProperties;
import com.datascraper.orchestrator.config.ScraperResilienceProperties;
import com.datascraper.orchestrator.model.ScraperSource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GoogleScraperClient extends AbstractScraperClient {

    public GoogleScraperClient(
            WebClient webClient,
            GoogleScraperProperties googleScraperProperties,
            ScraperResilienceProperties resilienceProperties) {
        super(webClient, googleScraperProperties.baseUrl(), ScraperSource.GOOGLE, resilienceProperties);
    }

}
