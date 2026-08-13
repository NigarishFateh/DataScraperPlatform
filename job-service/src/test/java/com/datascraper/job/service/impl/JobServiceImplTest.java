package com.datascraper.job.service.impl;

import com.datascraper.common.dto.job.CreateJobRequest;
import com.datascraper.common.dto.job.JobProgressUpdate;
import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;
import com.datascraper.job.entity.ScrapingJobEntity;
import com.datascraper.job.exception.InvalidJobStateException;
import com.datascraper.job.exception.JobNotFoundException;
import com.datascraper.job.repository.ScrapingJobRepository;
import com.datascraper.job.service.ExportNotifyClient;
import com.datascraper.job.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JobServiceImplTest {

    @Autowired
    private JobServiceImpl jobService;

    @Autowired
    private ScrapingJobRepository jobRepository;

    @MockBean
    private QueueService queueService;

    @MockBean
    private ExportNotifyClient exportNotifyClient;

    private CreateJobRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRequest = new CreateJobRequest(
                List.of("cat-1"),
                List.of("US"),
                List.of("city-1"),
                List.of("google-maps"),
                100,
                null,
                null
        );
    }

    @Test
    void createJob_setsQueuedStatusAndPublishesDiscoveryMessage() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");

        assertThat(created.status()).isEqualTo(JobStatus.QUEUED);
        assertThat(created.phase()).isEqualTo(JobPhase.CREATED);
        assertThat(created.userId()).isEqualTo("user-1");
        verify(queueService).publishDiscovery(any());
    }

    @Test
    void cancelJob_transitionsRunningJobToCancelled() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");
        jobService.updateProgress(created.id(), runningUpdate(created.id()));

        JobResponse cancelled = jobService.cancelJob(created.id(), "user-1");

        assertThat(cancelled.status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(cancelled.completedAt()).isNotNull();
    }

    @Test
    void cancelJob_rejectsCompletedJob() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");
        jobService.completeJob(created.id(), "export-1");

        assertThatThrownBy(() -> jobService.cancelJob(created.id(), "user-1"))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void pauseJob_transitionsRunningJobToPaused() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");
        jobService.updateProgress(created.id(), runningUpdate(created.id()));

        JobResponse paused = jobService.pauseJob(created.id(), "user-1");

        assertThat(paused.status()).isEqualTo(JobStatus.PAUSED);
    }

    @Test
    void pauseJob_rejectsQueuedJob() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");

        assertThatThrownBy(() -> jobService.pauseJob(created.id(), "user-1"))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void resumeJob_inDiscoveryPhaseRepublishesToQueue() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");
        jobService.updateProgress(created.id(), new JobProgressUpdate(
                created.id(),
                JobStatus.RUNNING,
                JobPhase.DISCOVERY,
                10, 5, 0, 0,
                5,
                null,
                "discovering",
                null,
                Instant.now()
        ));
        jobService.pauseJob(created.id(), "user-1");

        JobResponse resumed = jobService.resumeJob(created.id(), "user-1");

        assertThat(resumed.status()).isEqualTo(JobStatus.RUNNING);
        verify(queueService, times(2)).publishDiscovery(any());
    }

    @Test
    void resumeJob_inEnrichmentPhaseDoesNotRepublishToDiscoveryQueue() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");
        jobService.updateProgress(created.id(), new JobProgressUpdate(
                created.id(),
                JobStatus.RUNNING,
                JobPhase.ENRICHMENT,
                50, 20, 0, 0,
                20,
                null,
                "enriching",
                "checkpoint-1",
                Instant.now()
        ));
        jobService.pauseJob(created.id(), "user-1");

        JobResponse resumed = jobService.resumeJob(created.id(), "user-1");

        assertThat(resumed.status()).isEqualTo(JobStatus.RUNNING);
        verify(queueService, times(1)).publishDiscovery(any());
    }

    @Test
    void retryJob_clonesFailedJobAsNewQueuedJob() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");
        jobService.failJob(created.id(), "timeout");

        JobResponse retry = jobService.retryJob(created.id(), "user-1");

        assertThat(retry.id()).isNotEqualTo(created.id());
        assertThat(retry.status()).isEqualTo(JobStatus.QUEUED);
        assertThat(retry.phase()).isEqualTo(JobPhase.CREATED);
        assertThat(retry.checkpoint()).isNull();
        verify(queueService, times(2)).publishDiscovery(any());
    }

    @Test
    void retryJob_rejectsNonFailedJob() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");

        assertThatThrownBy(() -> jobService.retryJob(created.id(), "user-1"))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void completeJob_marksJobCompletedWithExportId() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");

        JobResponse completed = jobService.completeJob(created.id(), "export-99");

        assertThat(completed.status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(completed.phase()).isEqualTo(JobPhase.DONE);
        assertThat(completed.exportId()).isEqualTo("export-99");
        assertThat(completed.progressPercent()).isEqualTo(100);
    }

    @Test
    void failJob_marksJobFailedWithMessage() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");

        JobResponse failed = jobService.failJob(created.id(), "provider error");

        assertThat(failed.status()).isEqualTo(JobStatus.FAILED);
        assertThat(failed.errorMessage()).isEqualTo("provider error");
        assertThat(failed.completedAt()).isNotNull();
    }

    @Test
    void completeJob_onFailedJob_attachesExportIdWithoutCompleting() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");
        jobService.failJob(created.id(), "provider error");

        JobResponse attached = jobService.completeJob(created.id(), "export-partial");

        assertThat(attached.status()).isEqualTo(JobStatus.FAILED);
        assertThat(attached.exportId()).isEqualTo("export-partial");
    }

    @Test
    void updateProgress_calculatesEstimatedRemainingSeconds() {
        JobResponse created = jobService.createJob(sampleRequest, "user-1");
        jobService.updateProgress(created.id(), new JobProgressUpdate(
                created.id(),
                JobStatus.RUNNING,
                JobPhase.ENRICHMENT,
                50, 25, 0, 0,
                25,
                null,
                "enriching",
                null,
                Instant.now()
        ));

        ScrapingJobEntity job = jobRepository.findById(created.id()).orElseThrow();
        job.setStartedAt(Instant.now().minusSeconds(100));
        jobRepository.saveAndFlush(job);

        JobResponse updated = jobService.updateProgress(created.id(), new JobProgressUpdate(
                created.id(),
                JobStatus.RUNNING,
                JobPhase.ENRICHMENT,
                50, 25, 0, 0,
                25,
                null,
                "enriching",
                null,
                Instant.now()
        ));

        assertThat(updated.estimatedRemainingSeconds()).isNotNull();
        assertThat(updated.estimatedRemainingSeconds()).isGreaterThan(0L);
    }

    @Test
    void getJob_throwsWhenMissing() {
        assertThatThrownBy(() -> jobService.getJob(UUID.randomUUID()))
                .isInstanceOf(JobNotFoundException.class);
    }

    private JobProgressUpdate runningUpdate(UUID jobId) {
        return new JobProgressUpdate(
                jobId,
                JobStatus.RUNNING,
                JobPhase.DISCOVERY,
                0, 0, 0, 0,
                0,
                null,
                "started",
                null,
                Instant.now()
        );
    }
}
