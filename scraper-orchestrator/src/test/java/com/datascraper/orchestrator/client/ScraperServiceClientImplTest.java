/**
 * Tests HTTP scrape calls, correlation headers, and error mapping.
 */
package com.datascraper.orchestrator.client;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperExecutionStatus;
import com.datascraper.common.enums.ScraperType;
import com.datascraper.orchestrator.config.IntelligenceScraperProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScraperServiceClientImplTest {

    private MockWebServer mockWebServer;
    private ScraperServiceClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        IntelligenceScraperProperties properties = new IntelligenceScraperProperties();
        properties.getResilience().setTimeoutMs(5000);

        WebClient webClient = WebClient.builder().build();
        client = new ScraperServiceClientImpl(webClient, properties, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void scrapeReturnsSuccessResultAndPropagatesCorrelationId() throws Exception {
        ScraperResult expected = ScraperResult.success(
                ScraperType.COMPANY_WEBSITE,
                "ok",
                List.of(Map.of("title", "Acme")),
                Map.of("source", "test")
        );
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(expected)));

        ScraperContext context = new ScraperContext(
                "job-1", "co-1", "Acme", "https://acme.example", List.of(), "corr-abc");

        ScraperResult result = client.scrape(mockWebServer.url("/").toString(), ScraperType.COMPANY_WEBSITE, context);

        assertThat(result.status()).isEqualTo(ScraperExecutionStatus.SUCCESS);
        assertThat(result.items()).hasSize(1);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/scrape");
        assertThat(request.getHeader(ScraperServiceClientImpl.CORRELATION_HEADER)).isEqualTo("corr-abc");
    }

    @Test
    void scrapeMapsHttpErrorToFailedResult() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"status\":503,\"error\":\"Service Unavailable\"}"));

        ScraperContext context = new ScraperContext(
                "job-1", "co-1", "Acme", "https://acme.example", List.of(), "corr-1");

        ScraperResult result = client.scrape(mockWebServer.url("/").toString(), ScraperType.COMPANY_WEBSITE, context);

        assertThat(result.status()).isEqualTo(ScraperExecutionStatus.FAILED);
        assertThat(result.message()).contains("503");
    }

    @Test
    void scrapeParsesScraperResultFromErrorBody() throws Exception {
        ScraperResult errorBody = ScraperResult.failed(ScraperType.COMPANY_WEBSITE, "robots blocked");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(errorBody)));

        ScraperContext context = new ScraperContext(
                "job-1", "co-1", "Acme", "https://acme.example", List.of(), "corr-1");

        ScraperResult result = client.scrape(mockWebServer.url("/").toString(), ScraperType.COMPANY_WEBSITE, context);

        assertThat(result.status()).isEqualTo(ScraperExecutionStatus.FAILED);
        assertThat(result.message()).isEqualTo("robots blocked");
    }

    @Test
    void scrapeThrowsOnUnreachableHost() throws IOException {
        mockWebServer.shutdown();

        ScraperContext context = new ScraperContext(
                "job-1", "co-1", "Acme", "https://acme.example", List.of(), "corr-1");

        assertThatThrownBy(() -> client.scrape("http://127.0.0.1:1", ScraperType.COMPANY_WEBSITE, context))
                .isInstanceOf(ScraperCommunicationException.class)
                .hasMessageContaining("Network error");
    }
}
