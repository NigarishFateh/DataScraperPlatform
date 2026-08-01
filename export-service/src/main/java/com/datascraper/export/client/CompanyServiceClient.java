package com.datascraper.export.client;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.export.config.ServiceClientProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CompanyServiceClient {

    private static final int PAGE_SIZE = 500;

    private final RestClient restClient;
    private final ServiceClientProperties properties;

    public CompanyServiceClient(RestClient restClient, ServiceClientProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<EnrichedCompany> fetchCompaniesByJob(UUID jobId) {
        List<EnrichedCompany> companies = new ArrayList<>();
        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            PageResponse<EnrichedCompany> response = fetchPage(jobId, page);
            if (response == null || response.items() == null || response.items().isEmpty()) {
                break;
            }
            companies.addAll(response.items());
            hasMore = response.hasMore();
            page++;
        }

        return companies;
    }

    private PageResponse<EnrichedCompany> fetchPage(UUID jobId, int page) {
        try {
            return restClient.get()
                    .uri(properties.getCompany().getBaseUrl()
                            + "/api/companies/by-job/{jobId}?page={page}&pageSize={pageSize}",
                            jobId, page, PAGE_SIZE)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException ex) {
            return PageResponse.of(List.of(), page, PAGE_SIZE, 0);
        }
    }
}
