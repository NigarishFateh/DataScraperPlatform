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
                JobPhase.ENRICHMENT,
                count,
                null,
                null,
                null,
                25,
                count > 0 ? "Discovery completed — transitioning to enrichment phase" : "Discovery finished — advancing to next stage",
                null
        );
    }

    /** Live running total while providers are still discovering. */
    public static JobProgressPatchRequest discoveredProgress(int count) {
        int progress = Math.min(24, 8 + Math.min(count, 80) / 5);
        return new JobProgressPatchRequest(
                JobStatus.RUNNING,
                JobPhase.DISCOVERY,
                count,
                null,
                null,
                null,
                progress,
                "Discovered " + count + " companies so far",
                null
        );
    }
}
