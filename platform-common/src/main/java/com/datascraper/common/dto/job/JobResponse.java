package com.datascraper.common.dto.job;

import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full job status view for polling clients.
 */
public record JobResponse(
        UUID id,
        JobStatus status,
        JobPhase phase,
        String userId,
        List<String> categoryIds,
        List<String> countryCodes,
        List<String> cityIds,
        int discoveredCount,
        int enrichedCount,
        int persistedCount,
        int failedCount,
        int progressPercent,
        Long estimatedRemainingSeconds,
        String exportId,
        String errorMessage,
        String checkpoint,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant completedAt
) {
}
