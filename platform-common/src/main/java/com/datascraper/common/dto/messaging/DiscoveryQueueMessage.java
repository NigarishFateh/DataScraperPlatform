package com.datascraper.common.dto.messaging;

import java.util.List;
import java.util.UUID;

/**
 * Message published to the discovery queue when a job starts.
 */
public record DiscoveryQueueMessage(
        UUID jobId,
        String correlationId,
        String userId,
        List<String> countryCodes,
        List<String> cityIds,
        List<String> categoryIds,
        List<String> enabledProviders,
        int maxCompanies
) {
}
