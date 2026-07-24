/**
 * Lists possible statuses for an intelligence job.
 */
package com.datascraper.common.enums;

/**
 * Lifecycle of an async intelligence job (Phase 1 decision: Search is async).
 */
public enum IntelligenceJobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    PARTIAL,
    FAILED,
    CANCELLED
}
