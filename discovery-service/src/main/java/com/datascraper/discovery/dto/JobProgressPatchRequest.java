package com.datascraper.discovery.dto;

import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;

public record JobProgressPatchRequest(
        JobStatus status,
        JobPhase phase,
        Integer discoveredCount,
        Integer enrichedCount,
        Integer persistedCount,
        Integer failedCount,
        Integer progressPercent,
        String message,
        String checkpoint
) {
    public static JobProgressPatchRequest discoveryPhase(String message) {
        return new JobProgressPatchRequest(
                JobStatus.RUNNING,
                JobPhase.DISCOVERY,
                null,
                null,
                null,
                null,
                10,
                message,
                null
        );
    }

    public static JobProgressPatchRequest discoveredCount(int count) {
        return new JobProgressPatchRequest(
                JobStatus.RUNNING,
                JobPhase.DISCOVERY,
                count,
                null,
                null,
                null,
                25,
                "Discovery completed",
                null
        );
    }
}
