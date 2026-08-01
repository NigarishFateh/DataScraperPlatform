package com.datascraper.job.service;

import com.datascraper.common.dto.messaging.DiscoveryQueueMessage;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class InMemoryQueueService {

    private final ConcurrentLinkedQueue<DiscoveryQueueMessage> discoveryQueue;

    public InMemoryQueueService(ConcurrentLinkedQueue<DiscoveryQueueMessage> discoveryQueue) {
        this.discoveryQueue = discoveryQueue;
    }

    public void enqueue(DiscoveryQueueMessage message) {
        discoveryQueue.offer(message);
    }

    public Optional<DiscoveryQueueMessage> poll() {
        return Optional.ofNullable(discoveryQueue.poll());
    }

    public int size() {
        return discoveryQueue.size();
    }
}
