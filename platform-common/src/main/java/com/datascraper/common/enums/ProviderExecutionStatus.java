package com.datascraper.common.enums;

/**
 * Outcome of a single provider execution against one company.
 */
public enum ProviderExecutionStatus {
    SUCCESS,
    FAILED,
    SKIPPED,
    TIMEOUT,
    CIRCUIT_OPEN
}
