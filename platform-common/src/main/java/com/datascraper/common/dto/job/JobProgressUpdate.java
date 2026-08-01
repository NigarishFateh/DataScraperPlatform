package com.datascraper.common.dto.job;

import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Progress snapshot published during job execution.
 */
public record JobProgressUpdate(
        UUID jobId,
        JobStatus status,
        JobPhase phase,
        int discoveredCount,
        int enrichedCount,
        int persistedCount,
        int failedCount,
        int progressPercent,
        Long estimatedRemainingSeconds,
        String message,
        String checkpoint,
        Instant updatedAt
) {
}
