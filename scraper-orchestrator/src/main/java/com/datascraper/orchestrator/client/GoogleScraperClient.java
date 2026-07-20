package com.datascraper.orchestrator.client;

import com.datascraper.orchestrator.config.GoogleScraperProperties;
import com.datascraper.orchestrator.model.DataCategory;
import com.datascraper.orchestrator.model.ScrapedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleScraperClient {

    private final WebClient webClient;
    private final GoogleScraperProperties googleScraperProperties;

    public ScrapedData scrape(DataCategory category) {
        String url = googleScraperProperties.baseUrl() + "/api/scrape/" + category.name().toLowerCase();
        log.info("Calling Google scraper at {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(ScrapedData.class)
                .block();
    }

}
