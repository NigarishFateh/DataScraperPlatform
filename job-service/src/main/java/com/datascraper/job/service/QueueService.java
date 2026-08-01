package com.datascraper.job.service;

import com.datascraper.common.dto.messaging.DiscoveryQueueMessage;

import java.util.Optional;

public interface QueueService {

    void publishDiscovery(DiscoveryQueueMessage message);

    Optional<DiscoveryQueueMessage> pollDiscovery();

    int discoveryQueueSize();
}
