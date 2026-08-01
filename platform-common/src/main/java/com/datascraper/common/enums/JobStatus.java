package com.datascraper.common.enums;

/**
 * Lifecycle states for an asynchronous scraping job.
 */
public enum JobStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
