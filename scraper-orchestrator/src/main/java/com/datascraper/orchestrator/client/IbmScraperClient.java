/**
 * HTTP client that talks to the IBM scraper service.
 */
package com.datascraper.orchestrator.client;

import com.datascraper.orchestrator.config.IbmScraperProperties;
import com.datascraper.orchestrator.config.ScraperResilienceProperties;
import com.datascraper.orchestrator.model.ScraperSource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class IbmScraperClient extends AbstractScraperClient {

    public IbmScraperClient(
            WebClient webClient,
            IbmScraperProperties ibmScraperProperties,
            ScraperResilienceProperties resilienceProperties) {
        super(webClient, ibmScraperProperties.baseUrl(), ScraperSource.IBM, resilienceProperties);
    }

}
