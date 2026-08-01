package com.datascraper.common.enums;

/**
 * Pipeline phases a scraping job progresses through.
 */
public enum JobPhase {
    CREATED,
    DISCOVERY,
    ENRICHMENT,
    AGGREGATION,
    NORMALIZATION,
    VALIDATION,
    PERSISTENCE,
    EXPORT,
    DONE
}
