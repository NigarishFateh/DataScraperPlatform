package com.datascraper.orchestrator.client;

import com.datascraper.orchestrator.config.MicrosoftScraperProperties;
import com.datascraper.orchestrator.dto.MicrosoftScrapeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class MicrosoftScraperClient {

    private final WebClient webClient;
    private final MicrosoftScraperProperties microsoftScraperProperties;

    public MicrosoftScrapeResult scrapeJobs() {
        String url = microsoftScraperProperties.baseUrl() + "/api/scrape/jobs";
        log.info("Calling Microsoft scraper at {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(MicrosoftScrapeResult.class)
                .block();
    }

}
