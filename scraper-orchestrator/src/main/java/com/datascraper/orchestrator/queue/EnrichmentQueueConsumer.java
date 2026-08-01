package com.datascraper.orchestrator.queue;

import com.datascraper.orchestrator.config.OrchestratorProperties;
import com.datascraper.orchestrator.service.CompanyEnrichmentPipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.queue", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EnrichmentQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentQueueConsumer.class);

    private final EnrichmentQueuePort enrichmentQueuePort;
    private final CompanyEnrichmentPipelineService pipelineService;

    public EnrichmentQueueConsumer(
            EnrichmentQueuePort enrichmentQueuePort,
            CompanyEnrichmentPipelineService pipelineService
    ) {
        this.enrichmentQueuePort = enrichmentQueuePort;
        this.pipelineService = pipelineService;
    }

    @Scheduled(fixedDelayString = "${app.queue.poll-interval-ms:1000}")
    public void pollEnrichmentQueue() {
        enrichmentQueuePort.poll().ifPresent(message -> {
            log.info("Processing enrichment message for job {} company {}",
                    message.jobId(), message.company().name());
            try {
                pipelineService.process(message);
            } catch (Exception ex) {
                log.error("Failed enrichment for job {} company {}: {}",
                        message.jobId(), message.company().name(), ex.getMessage(), ex);
            }
        });
    }
}
