package com.datascraper.export.client;

import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.export.config.ServiceClientProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class JobServiceClient {

    private final RestClient restClient;
    private final ServiceClientProperties properties;

    public JobServiceClient(RestClient restClient, ServiceClientProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public JobResponse fetchJob(UUID jobId) {
        try {
            JobResponse response = restClient.get()
                    .uri(properties.getJob().getBaseUrl() + "/api/jobs/{id}", jobId)
                    .retrieve()
                    .body(JobResponse.class);
            if (response == null) {
                throw new IllegalStateException("Job service returned empty response for job " + jobId);
            }
            return response;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to fetch job " + jobId + ": " + ex.getMessage(), ex);
        }
    }

    public void completeJob(UUID jobId, UUID exportId) {
        try {
            restClient.post()
                    .uri(properties.getJob().getBaseUrl() + "/api/jobs/{id}/complete", jobId)
                    .body(CompleteJobRequest.of(exportId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Failed to notify job completion for job " + jobId + ": " + ex.getMessage(),
                    ex
            );
        }
    }
}
