package com.datascraper.job.service;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.common.dto.job.CreateJobRequest;
import com.datascraper.common.dto.job.JobProgressUpdate;
import com.datascraper.common.dto.job.JobResponse;

import java.util.UUID;

public interface JobService {

    JobResponse createJob(CreateJobRequest request, String userId);

    JobResponse getJob(UUID jobId);

    PageResponse<JobResponse> listJobs(String userId, int page, int pageSize);

    JobResponse cancelJob(UUID jobId, String userId);

    JobResponse pauseJob(UUID jobId, String userId);

    JobResponse resumeJob(UUID jobId, String userId);

    JobResponse retryJob(UUID jobId, String userId);

    JobResponse updateProgress(UUID jobId, JobProgressUpdate update);

    JobResponse completeJob(UUID jobId, String exportId);

    JobResponse failJob(UUID jobId, String errorMessage);
}
