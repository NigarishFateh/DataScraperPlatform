package com.datascraper.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class GatewayInfoConfig {

    @Bean
    public RouterFunction<ServerResponse> gatewayRoot() {
        Map<String, Object> body = Map.of(
                "service", "Global Business Intelligence Platform gateway",
                "status", "up",
                "message", "This is the API, not the app UI. Open the Chrome extension side panel instead.",
                "extension", "chrome://extensions → Load unpacked → chrome-extension/dist"
        );
        return route(GET("/"), request ->
                ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body));
    }
}
