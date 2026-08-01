package com.datascraper.discovery.client;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.discovery.config.AppProperties;
import com.datascraper.discovery.dto.CategoryDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class CategoryCatalogClient {

    private final WebClient webClient;

    public CategoryCatalogClient(WebClient.Builder webClientBuilder, AppProperties appProperties) {
        this.webClient = webClientBuilder.baseUrl(trimTrailingSlash(appProperties.getCategoryServiceUri())).build();
    }

    public List<CategoryDto> listByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        PageResponse<CategoryDto> page = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/categories");
                    for (String id : ids) {
                        uriBuilder.queryParam("ids", id);
                    }
                    uriBuilder.queryParam("page", 0);
                    uriBuilder.queryParam("pageSize", Math.max(ids.size(), 50));
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PageResponse<CategoryDto>>() {
                })
                .blockOptional()
                .orElse(null);

        if (page == null || page.items() == null) {
            return List.of();
        }
        return page.items();
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8084";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
