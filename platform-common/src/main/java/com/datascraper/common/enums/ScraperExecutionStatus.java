/**
 * Lists possible outcomes for one scraper run.
 */
package com.datascraper.common.enums;

/**
 * Outcome of a single scraper strategy execution.
 */
public enum ScraperExecutionStatus {
    SUCCESS,
    PARTIAL,
    FAILED,
    SKIPPED
}
