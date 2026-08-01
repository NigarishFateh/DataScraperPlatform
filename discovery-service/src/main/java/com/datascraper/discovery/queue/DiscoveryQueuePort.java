package com.datascraper.discovery.queue;

import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;
import com.datascraper.common.dto.messaging.DiscoveryQueueMessage;
import com.datascraper.common.queue.PlatformQueues;

import java.util.Optional;

public interface DiscoveryQueuePort {

    Optional<DiscoveryQueueMessage> poll();

    void enqueueEnrichment(CompanyEnrichmentMessage message);

    String discoveryQueueKey();

    default String enrichmentQueueKey() {
        return PlatformQueues.COMPANY_ENRICHMENT;
    }
}
