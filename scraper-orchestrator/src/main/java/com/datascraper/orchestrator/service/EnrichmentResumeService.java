package com.datascraper.orchestrator.service;

import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;
import com.datascraper.orchestrator.client.JobServiceClient;
import com.datascraper.orchestrator.queue.EnrichmentQueuePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class EnrichmentResumeService {

    private final JobServiceClient jobServiceClient;
    private final EnrichmentQueuePort enrichmentQueuePort;

    public EnrichmentResumeService(JobServiceClient jobServiceClient, EnrichmentQueuePort enrichmentQueuePort) {
        this.jobServiceClient = jobServiceClient;
        this.enrichmentQueuePort = enrichmentQueuePort;
    }

    public JobResponse resume(UUID jobId) {
        JobResponse job = jobServiceClient.getJob(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        if (job.status() != JobStatus.RUNNING && job.status() != JobStatus.PAUSED) {
            throw new IllegalStateException("Job is not resumable in status " + job.status());
        }

        log.info("Resuming enrichment for job {} from checkpoint {}", jobId, job.checkpoint());
        if (job.phase() == JobPhase.ENRICHMENT || job.phase() == JobPhase.AGGREGATION
                || job.phase() == JobPhase.NORMALIZATION || job.phase() == JobPhase.VALIDATION
                || job.phase() == JobPhase.PERSISTENCE) {
            enrichmentQueuePort.signalResume(jobId);
        }
        return job;
    }
}
