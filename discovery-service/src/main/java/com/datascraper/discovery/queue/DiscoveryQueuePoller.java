package com.datascraper.discovery.queue;

import com.datascraper.discovery.config.AppProperties;
import com.datascraper.discovery.service.DiscoveryOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryQueuePoller {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryQueuePoller.class);

    private final DiscoveryQueuePort discoveryQueuePort;
    private final DiscoveryOrchestrationService orchestrationService;
    private final AppProperties appProperties;

    public DiscoveryQueuePoller(
            DiscoveryQueuePort discoveryQueuePort,
            DiscoveryOrchestrationService orchestrationService,
            AppProperties appProperties
    ) {
        this.discoveryQueuePort = discoveryQueuePort;
        this.orchestrationService = orchestrationService;
        this.appProperties = appProperties;
    }

    @Scheduled(fixedDelayString = "${app.queue.poll-interval-ms:1000}")
    public void pollDiscoveryQueue() {
        discoveryQueuePort.poll().ifPresent(message -> {
            log.info("Processing discovery queue message for job {}", message.jobId());
            try {
                orchestrationService.processQueueMessage(message);
            } catch (Exception ex) {
                log.error("Failed to process discovery message for job {}: {}", message.jobId(), ex.getMessage(), ex);
            }
        });
    }
}
