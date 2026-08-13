package com.datascraper.orchestrator.job;

import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;
import com.datascraper.orchestrator.client.ExportTriggerClient;
import com.datascraper.orchestrator.client.JobServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * If enrichment stops updating (hung website scrape, dead worker), fail the job with a
 * message so the user gets an error plus a partial Excel of whatever was already saved.
 * Also finishes jobs that enriched every company but never got an export file.
 */
@Component
public class EnrichmentStallWatchdog {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentStallWatchdog.class);
    private static final Duration STALL_AFTER = Duration.ofMinutes(2);

    private final JobServiceClient jobServiceClient;
    private final ExportTriggerClient exportTriggerClient;

    public EnrichmentStallWatchdog(
            JobServiceClient jobServiceClient,
            ExportTriggerClient exportTriggerClient
    ) {
        this.jobServiceClient = jobServiceClient;
        this.exportTriggerClient = exportTriggerClient;
    }

    @Scheduled(fixedDelay = 20_000)
    public void failStalledJobs() {
        List<JobResponse> running;
        try {
            running = jobServiceClient.listRunningJobs();
        } catch (Exception ex) {
            log.debug("Stall watchdog skipped: {}", ex.getMessage());
            return;
        }
        Instant cutoff = Instant.now().minus(STALL_AFTER);
        for (JobResponse job : running) {
            if (job == null || job.id() == null) {
                continue;
            }
            if (job.status() != JobStatus.RUNNING) {
                continue;
            }
            Instant updated = job.updatedAt() != null ? job.updatedAt() : job.startedAt();
            if (updated == null || updated.isAfter(cutoff)) {
                continue;
            }
            boolean hasExport = job.exportId() != null && !job.exportId().isBlank();
            boolean enrichmentFinished = job.discoveredCount() > 0
                    && job.enrichedCount() >= job.discoveredCount();

            if (enrichmentFinished && job.persistedCount() > 0 && !hasExport) {
                log.warn("Job {} enrichment finished but export never started — triggering Excel", job.id());
                try {
                    exportTriggerClient.triggerExport(job.id());
                } catch (Exception ex) {
                    log.warn("Late export trigger failed for {}: {}", job.id(), ex.getMessage());
                }
                continue;
            }

            if (job.phase() != JobPhase.ENRICHMENT || enrichmentFinished) {
                continue;
            }

            String message = "Scrape stalled during enrichment after "
                    + job.enrichedCount() + "/" + job.discoveredCount()
                    + " companies. Download the Excel of rows saved so far, then retry if needed.";
            log.warn("Failing stalled job {}: last update {}", job.id(), updated);
            jobServiceClient.failJob(job.id(), message);
        }
    }
}
