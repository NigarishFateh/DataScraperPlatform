package com.datascraper.discovery.client;

import com.datascraper.discovery.config.AppProperties;
import com.datascraper.discovery.dto.CompanyPageDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class CompanyCatalogClient {

    private final WebClient webClient;

    public CompanyCatalogClient(WebClient.Builder webClientBuilder, AppProperties appProperties) {
        this.webClient = webClientBuilder
                .baseUrl(trimTrailingSlash(appProperties.getCompanyServiceUri()))
                .build();
    }

    public CompanyPageDto search(
            List<String> cityIds,
            String search,
            List<String> categoryIds,
            int page,
            int pageSize
    ) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/companies/search")
                            .queryParam("search", search == null ? "" : search)
                            .queryParam("page", page)
                            .queryParam("pageSize", pageSize);
                    if (cityIds != null) {
                        for (String cityId : cityIds) {
                            if (cityId != null && !cityId.isBlank()) {
                                uriBuilder.queryParam("cityIds", cityId);
                            }
                        }
                    }
                    if (categoryIds != null) {
                        for (String categoryId : categoryIds) {
                            if (categoryId != null && !categoryId.isBlank()) {
                                uriBuilder.queryParam("categoryIds", categoryId);
                            }
                        }
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(CompanyPageDto.class)
                .blockOptional()
                .orElse(new CompanyPageDto(List.of(), page, pageSize, 0, false));
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8083";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
