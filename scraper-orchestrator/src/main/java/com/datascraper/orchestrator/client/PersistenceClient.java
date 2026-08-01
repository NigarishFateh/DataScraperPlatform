package com.datascraper.orchestrator.client;

import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.orchestrator.config.OrchestratorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

@Slf4j
@Component
public class PersistenceClient {

    public static final String JOB_ID_HEADER = "X-Job-Id";

    private final WebClient webClient;
    private final OrchestratorProperties properties;

    public PersistenceClient(WebClient webClient, OrchestratorProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public boolean persist(UUID jobId, EnrichedCompany company) {
        String url = normalizeBaseUrl(properties.getCompanyServiceUri()) + "/api/companies/enriched";
        try {
            webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(JOB_ID_HEADER, jobId.toString())
                    .bodyValue(company)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (WebClientResponseException ex) {
            log.warn("Persistence failed for job {} company {}: HTTP {} — {}",
                    jobId, company.id(), ex.getStatusCode(), ex.getResponseBodyAsString());
            return false;
        } catch (Exception ex) {
            log.warn("Persistence failed for job {} company {}: {}", jobId, company.id(), ex.getMessage());
            return false;
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
