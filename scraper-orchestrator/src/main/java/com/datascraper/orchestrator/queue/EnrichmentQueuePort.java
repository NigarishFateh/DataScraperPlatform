package com.datascraper.orchestrator.queue;

import com.datascraper.common.dto.messaging.CompanyEnrichmentMessage;

import java.util.Optional;
import java.util.UUID;

public interface EnrichmentQueuePort {

    Optional<CompanyEnrichmentMessage> poll();

    void offer(CompanyEnrichmentMessage message);

    void signalResume(UUID jobId);

    String queueKey();
}
