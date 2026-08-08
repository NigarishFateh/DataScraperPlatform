package com.datascraper.discovery.dto;

import java.util.List;

/**
 * Batch response for NL restaurant leadership lookup.
 */
public record LeadershipLookupResponse(
        int requested,
        int found,
        String notes,
        List<LeadershipPersonResponse> results
) {
}
