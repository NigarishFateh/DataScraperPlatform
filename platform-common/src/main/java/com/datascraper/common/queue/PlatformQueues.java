package com.datascraper.common.queue;

/**
 * Redis / in-memory queue key constants shared across services.
 */
public final class PlatformQueues {

    public static final String DISCOVERY = "bi:queue:discovery";
    public static final String COMPANY_ENRICHMENT = "bi:queue:company";
    public static final String EXPORT = "bi:queue:export";
    public static final String JOB_PROGRESS = "bi:channel:job-progress";

    private PlatformQueues() {
    }
}
