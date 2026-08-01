package com.datascraper.job.service.impl;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.common.dto.job.CreateJobRequest;
import com.datascraper.common.dto.job.JobProgressUpdate;
import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.common.dto.messaging.DiscoveryQueueMessage;
import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;
import com.datascraper.job.entity.AuditLogEntity;
import com.datascraper.job.entity.ScrapingJobEntity;
import com.datascraper.job.entity.ScrapingJobProgressEntity;
import com.datascraper.job.exception.InvalidJobStateException;
import com.datascraper.job.exception.JobNotFoundException;
import com.datascraper.job.mapper.JobMapper;
import com.datascraper.job.repository.AuditLogRepository;
import com.datascraper.job.repository.ScrapingJobProgressRepository;
import com.datascraper.job.repository.ScrapingJobRepository;
import com.datascraper.job.service.JobService;
import com.datascraper.job.service.OrchestratorClient;
import com.datascraper.job.service.QueueService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    private static final Set<JobStatus> CANCELLABLE = EnumSet.of(
            JobStatus.QUEUED, JobStatus.RUNNING, JobStatus.PAUSED
    );
    private static final Set<JobPhase> DISCOVERY_PHASES = EnumSet.of(
            JobPhase.CREATED, JobPhase.DISCOVERY
    );

    private final ScrapingJobRepository jobRepository;
    private final ScrapingJobProgressRepository progressRepository;
    private final AuditLogRepository auditLogRepository;
    private final QueueService queueService;
    private final OrchestratorClient orchestratorClient;
    private final JobMapper jobMapper;
    private final ObjectMapper objectMapper;

    public JobServiceImpl(
            ScrapingJobRepository jobRepository,
            ScrapingJobProgressRepository progressRepository,
            AuditLogRepository auditLogRepository,
            QueueService queueService,
            OrchestratorClient orchestratorClient,
            JobMapper jobMapper,
            ObjectMapper objectMapper
    ) {
        this.jobRepository = jobRepository;
        this.progressRepository = progressRepository;
        this.auditLogRepository = auditLogRepository;
        this.queueService = queueService;
        this.orchestratorClient = orchestratorClient;
        this.jobMapper = jobMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobResponse createJob(CreateJobRequest request, String userId) {
        UUID jobId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();

        ScrapingJobEntity job = new ScrapingJobEntity();
        job.setId(jobId);
        job.setUserId(userId);
        job.setStatus(JobStatus.QUEUED);
        job.setPhase(JobPhase.CREATED);
        job.setCategoryIds(List.copyOf(request.categoryIds()));
        job.setCountryCodes(List.copyOf(request.countryCodes()));
        job.setCityIds(List.copyOf(request.cityIds()));
        job.setEnabledProviders(List.copyOf(request.enabledProviders()));
        job.setMaxCompanies(request.maxCompanies());
        job.setCorrelationId(correlationId);

        jobRepository.save(job);
        writeAudit(job, "JOB_CREATED", "Job queued for discovery");

        DiscoveryQueueMessage message = new DiscoveryQueueMessage(
                jobId,
                correlationId,
                userId,
                request.countryCodes(),
                request.cityIds(),
                request.categoryIds(),
                request.enabledProviders(),
                request.maxCompanies()
        );
        queueService.publishDiscovery(message);

        return jobMapper.toResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        return jobMapper.toResponse(requireJob(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<JobResponse> listJobs(String userId, int page, int pageSize) {
        int safePage = Math.max(page, 0);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<ScrapingJobEntity> result = jobRepository.findByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(safePage, safePageSize)
        );
        List<JobResponse> items = result.getContent().stream()
                .map(jobMapper::toResponse)
                .toList();
        return PageResponse.of(items, safePage, safePageSize, result.getTotalElements());
    }

    @Override
    public JobResponse cancelJob(UUID jobId, String userId) {
        ScrapingJobEntity job = requireJob(jobId);
        if (!CANCELLABLE.contains(job.getStatus())) {
            throw new InvalidJobStateException("Job cannot be cancelled in status " + job.getStatus());
        }
        job.setStatus(JobStatus.CANCELLED);
        job.setCompletedAt(Instant.now());
        writeAudit(job, "JOB_CANCELLED", "Cancelled by user " + userId);
        return jobMapper.toResponse(job);
    }

    @Override
    public JobResponse pauseJob(UUID jobId, String userId) {
        ScrapingJobEntity job = requireJob(jobId);
        if (job.getStatus() != JobStatus.RUNNING) {
            throw new InvalidJobStateException("Only running jobs can be paused");
        }
        job.setStatus(JobStatus.PAUSED);
        writeAudit(job, "JOB_PAUSED", "Paused by user " + userId);
        return jobMapper.toResponse(job);
    }

    @Override
    public JobResponse resumeJob(UUID jobId, String userId) {
        ScrapingJobEntity job = requireJob(jobId);
        if (job.getStatus() != JobStatus.PAUSED) {
            throw new InvalidJobStateException("Only paused jobs can be resumed");
        }
        job.setStatus(JobStatus.RUNNING);
        writeAudit(job, "JOB_RESUMED", "Resumed by user " + userId);

        if (DISCOVERY_PHASES.contains(job.getPhase())) {
            republishDiscovery(job);
        } else {
            orchestratorClient.notifyResume(jobId, job.getCorrelationId());
        }

        return jobMapper.toResponse(job);
    }

    @Override
    public JobResponse retryJob(UUID jobId, String userId) {
        ScrapingJobEntity source = requireJob(jobId);
        if (source.getStatus() != JobStatus.FAILED) {
            throw new InvalidJobStateException("Only failed jobs can be retried");
        }

        UUID newJobId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();

        ScrapingJobEntity retry = new ScrapingJobEntity();
        retry.setId(newJobId);
        retry.setUserId(source.getUserId());
        retry.setStatus(JobStatus.QUEUED);
        retry.setPhase(JobPhase.CREATED);
        retry.setCategoryIds(List.copyOf(source.getCategoryIds()));
        retry.setCountryCodes(List.copyOf(source.getCountryCodes()));
        retry.setCityIds(List.copyOf(source.getCityIds()));
        retry.setEnabledProviders(List.copyOf(source.getEnabledProviders()));
        retry.setMaxCompanies(source.getMaxCompanies());
        retry.setCheckpoint(source.getCheckpoint());
        retry.setCorrelationId(correlationId);

        jobRepository.save(retry);
        writeAudit(retry, "JOB_RETRIED", "Retried from failed job " + jobId + " by user " + userId);

        DiscoveryQueueMessage message = new DiscoveryQueueMessage(
                newJobId,
                correlationId,
                retry.getUserId(),
                retry.getCountryCodes(),
                retry.getCityIds(),
                retry.getCategoryIds(),
                retry.getEnabledProviders(),
                retry.getMaxCompanies()
        );
        queueService.publishDiscovery(message);

        return jobMapper.toResponse(retry);
    }

    @Override
    public JobResponse updateProgress(UUID jobId, JobProgressUpdate update) {
        ScrapingJobEntity job = requireJob(jobId);

        if (update.status() != null) {
            job.setStatus(update.status());
        }
        if (update.phase() != null) {
            job.setPhase(update.phase());
        }

        job.setDiscoveredCount(Math.max(job.getDiscoveredCount(), update.discoveredCount()));
        job.setEnrichedCount(Math.max(job.getEnrichedCount(), update.enrichedCount()));
        job.setPersistedCount(Math.max(job.getPersistedCount(), update.persistedCount()));
        job.setFailedCount(Math.max(job.getFailedCount(), update.failedCount()));
        if (update.progressPercent() > 0) {
            job.setProgressPercent(Math.max(job.getProgressPercent(), update.progressPercent()));
        }
        if (update.checkpoint() != null) {
            job.setCheckpoint(update.checkpoint());
        }

        if (job.getStartedAt() == null && job.getStatus() == JobStatus.RUNNING) {
            job.setStartedAt(Instant.now());
        }

        Long eta = update.estimatedRemainingSeconds();
        if (eta == null) {
            eta = estimateRemainingSeconds(job);
        }
        job.setEstimatedRemainingSeconds(eta);

        if (update.updatedAt() != null) {
            job.setUpdatedAt(update.updatedAt());
        }

        ScrapingJobProgressEntity progress = new ScrapingJobProgressEntity();
        progress.setId(UUID.randomUUID());
        progress.setJobId(jobId);
        progress.setPhase(update.phase() != null ? update.phase() : job.getPhase());
        progress.setMessage(update.message());
        progress.setCounts(buildCountsJson(update));
        progressRepository.save(progress);

        return jobMapper.toResponse(job);
    }

    @Override
    public JobResponse completeJob(UUID jobId, String exportId) {
        ScrapingJobEntity job = requireJob(jobId);
        if (job.getStatus() == JobStatus.CANCELLED || job.getStatus() == JobStatus.FAILED) {
            throw new InvalidJobStateException("Job cannot be completed in status " + job.getStatus());
        }

        job.setStatus(JobStatus.COMPLETED);
        job.setPhase(JobPhase.DONE);
        job.setProgressPercent(100);
        job.setEstimatedRemainingSeconds(0L);
        if (exportId != null && !exportId.isBlank()) {
            job.setExportId(exportId);
        }
        job.setCompletedAt(Instant.now());
        writeAudit(job, "JOB_COMPLETED", exportId != null ? "exportId=" + exportId : null);

        return jobMapper.toResponse(job);
    }

    @Override
    public JobResponse failJob(UUID jobId, String errorMessage) {
        ScrapingJobEntity job = requireJob(jobId);
        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.CANCELLED) {
            throw new InvalidJobStateException("Job cannot be failed in status " + job.getStatus());
        }
        if (job.getStatus() == JobStatus.FAILED) {
            return jobMapper.toResponse(job);
        }

        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setCompletedAt(Instant.now());
        writeAudit(job, "JOB_FAILED", errorMessage);

        return jobMapper.toResponse(job);
    }

    Long estimateRemainingSeconds(ScrapingJobEntity job) {
        if (job.getStartedAt() == null || job.getEnrichedCount() <= 0 || job.getMaxCompanies() <= job.getEnrichedCount()) {
            return null;
        }
        long elapsedSeconds = Duration.between(job.getStartedAt(), Instant.now()).getSeconds();
        if (elapsedSeconds <= 0) {
            return null;
        }
        double rate = (double) job.getEnrichedCount() / elapsedSeconds;
        if (rate <= 0) {
            return null;
        }
        int remaining = job.getMaxCompanies() - job.getEnrichedCount();
        return Math.max(1L, Math.round(remaining / rate));
    }

    private void republishDiscovery(ScrapingJobEntity job) {
        DiscoveryQueueMessage message = new DiscoveryQueueMessage(
                job.getId(),
                job.getCorrelationId(),
                job.getUserId(),
                job.getCountryCodes(),
                job.getCityIds(),
                job.getCategoryIds(),
                job.getEnabledProviders(),
                job.getMaxCompanies()
        );
        queueService.publishDiscovery(message);
    }

    private ScrapingJobEntity requireJob(UUID jobId) {
        return jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    }

    private void writeAudit(ScrapingJobEntity job, String action, String details) {
        AuditLogEntity audit = new AuditLogEntity();
        audit.setId(UUID.randomUUID());
        audit.setJobId(job.getId());
        audit.setUserId(job.getUserId());
        audit.setAction(action);
        audit.setDetails(details);
        auditLogRepository.save(audit);
    }

    private String buildCountsJson(JobProgressUpdate update) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("discovered", update.discoveredCount());
        counts.put("enriched", update.enrichedCount());
        counts.put("persisted", update.persistedCount());
        counts.put("failed", update.failedCount());
        counts.put("progressPercent", update.progressPercent());
        try {
            return objectMapper.writeValueAsString(counts);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize progress counts", ex);
        }
    }
}
