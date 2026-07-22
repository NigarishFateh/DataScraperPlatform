package com.datascraper.orchestrator.client;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * WebClient-based REST client — maps HTTP/JSON to {@link ScraperResult}.
 */
@Slf4j
@Component
public class ScraperServiceClientImpl implements ScraperServiceClient {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final WebClient webClient;
    private final IntelligenceScraperProperties properties;
    private final ObjectMapper objectMapper;

    public ScraperServiceClientImpl(
            WebClient webClient,
            IntelligenceScraperProperties properties,
            ObjectMapper objectMapper
    ) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ScraperResult scrape(String baseUrl, ScraperType scraperType, ScraperContext context) {
        String url = normalizeBaseUrl(baseUrl) + "/api/scrape";
        String correlationId = context.correlationId() != null ? context.correlationId() : "n/a";

        log.debug("POST {} type={} correlationId={}", url, scraperType, correlationId);

        try {
            ScraperResult result = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(CORRELATION_HEADER, correlationId)
                    .bodyValue(context)
                    .retrieve()
                    .bodyToMono(ScraperResult.class)
                    .timeout(Duration.ofMillis(properties.getResilience().getTimeoutMs()))
                    .block();

            if (result == null) {
                throw new ScraperCommunicationException("Empty response body from " + scraperType);
            }
            return result;
        } catch (WebClientResponseException ex) {
            return mapHttpError(scraperType, ex);
        } catch (WebClientRequestException ex) {
            throw new ScraperCommunicationException(
                    "Network error calling " + scraperType + " scraper: " + ex.getMessage(),
                    ex
            );
        }
    }

    private ScraperResult mapHttpError(ScraperType scraperType, WebClientResponseException ex) {
        String responseBody = ex.getResponseBodyAsString();
        ScraperResult body = tryParseScraperResult(responseBody);
        if (body != null) {
            return body;
        }
        String message = "HTTP %d from %s scraper".formatted(ex.getStatusCode().value(), scraperType);
        log.warn("{} — body: {}", message, truncate(responseBody));
        return ScraperResult.failed(scraperType, message);
    }

    private ScraperResult tryParseScraperResult(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ScraperResult.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 200 ? value : value.substring(0, 200) + "...";
    }
}
