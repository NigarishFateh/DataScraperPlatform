package com.datascraper.orchestrator.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({
        GoogleScraperProperties.class,
        MicrosoftScraperProperties.class,
        IbmScraperProperties.class,
        ScraperResilienceProperties.class,
        IntelligenceScraperProperties.class
})
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder, ScraperResilienceProperties resilienceProperties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(resilienceProperties.timeoutMs()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, resilienceProperties.timeoutMs());

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

}
