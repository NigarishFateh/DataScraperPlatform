package com.datascraper.discovery.client;

import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.discovery.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cross-process enrichment dispatch so discovery works without a shared Redis instance.
 */
@Component
public class OrchestratorEnrichmentClient {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorEnrichmentClient.class);

    private final WebClient webClient;
    private final String orchestratorBaseUrl;
    private final ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "enrichment-dispatch");
        t.setDaemon(true);
        return t;
    });

    public OrchestratorEnrichmentClient(WebClient.Builder webClientBuilder, AppProperties appProperties) {
        this.webClient = webClientBuilder.build();
        this.orchestratorBaseUrl = trimTrailingSlash(
                appProperties.getOrchestratorServiceUri() == null
                        ? "http://localhost:8085"
                        : appProperties.getOrchestratorServiceUri()
        );
    }

    /**
     * Fire-and-forget enrichment so discovery can finish quickly.
     */
    public void enrichAsync(CompanyEnrichmentMessage message) {
        executor.execute(() -> enrich(message));
    }

    public void enrich(CompanyEnrichmentMessage message) {
        try {
            webClient.post()
                    .uri(orchestratorBaseUrl + "/api/orchestrator/enrich")
                    .bodyValue(message)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofMinutes(5));
        } catch (Exception ex) {
            log.warn(
                    "Failed to dispatch enrichment for job {} company {}: {}",
                    message.jobId(),
                    message.company() == null ? "?" : message.company().name(),
                    ex.getMessage()
            );
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
