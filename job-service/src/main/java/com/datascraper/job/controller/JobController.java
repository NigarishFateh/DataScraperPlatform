package com.datascraper.job.controller;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.common.dto.job.CreateJobRequest;
import com.datascraper.common.dto.job.JobProgressUpdate;
import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.job.dto.CompleteJobRequest;
import com.datascraper.job.dto.FailJobRequest;
import com.datascraper.job.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String DEFAULT_USER = "anonymous";

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/running")
    public java.util.List<JobResponse> listRunningJobs() {
        return jobService.listRunningJobs();
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @RequestHeader(value = USER_ID_HEADER, defaultValue = DEFAULT_USER) String userId
    ) {
        JobResponse response = jobService.createJob(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable UUID id) {
        return jobService.getJob(id);
    }

    @GetMapping
    public PageResponse<JobResponse> listJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = USER_ID_HEADER, defaultValue = DEFAULT_USER) String userId
    ) {
        return jobService.listJobs(userId, page, pageSize);
    }

    @PostMapping("/{id}/cancel")
    public JobResponse cancelJob(
            @PathVariable UUID id,
            @RequestHeader(value = USER_ID_HEADER, defaultValue = DEFAULT_USER) String userId
    ) {
        return jobService.cancelJob(id, userId);
    }

    @PostMapping("/{id}/pause")
    public JobResponse pauseJob(
            @PathVariable UUID id,
            @RequestHeader(value = USER_ID_HEADER, defaultValue = DEFAULT_USER) String userId
    ) {
        return jobService.pauseJob(id, userId);
    }

    @PostMapping("/{id}/resume")
    public JobResponse resumeJob(
            @PathVariable UUID id,
            @RequestHeader(value = USER_ID_HEADER, defaultValue = DEFAULT_USER) String userId
    ) {
        return jobService.resumeJob(id, userId);
    }

    @PostMapping("/{id}/retry")
    public JobResponse retryJob(
            @PathVariable UUID id,
            @RequestHeader(value = USER_ID_HEADER, defaultValue = DEFAULT_USER) String userId
    ) {
        return jobService.retryJob(id, userId);
    }

    @PatchMapping("/{id}/progress")
    public JobResponse updateProgress(
            @PathVariable UUID id,
            @Valid @RequestBody JobProgressUpdate update
    ) {
        JobProgressUpdate normalized = new JobProgressUpdate(
                id,
                update.status(),
                update.phase(),
                update.discoveredCount(),
                update.enrichedCount(),
                update.persistedCount(),
                update.failedCount(),
                update.progressPercent(),
                update.estimatedRemainingSeconds(),
                update.message(),
                update.checkpoint(),
                update.updatedAt()
        );
        return jobService.updateProgress(id, normalized);
    }

    @PostMapping("/{id}/complete")
    public JobResponse completeJob(
            @PathVariable UUID id,
            @RequestBody(required = false) CompleteJobRequest request
    ) {
        String exportId = request != null ? request.exportId() : null;
        return jobService.completeJob(id, exportId);
    }

    @PostMapping("/{id}/fail")
    public JobResponse failJob(
            @PathVariable UUID id,
            @RequestBody(required = false) FailJobRequest request
    ) {
        String errorMessage = request != null ? request.errorMessage() : "Job failed";
        return jobService.failJob(id, errorMessage);
    }
}
