package com.datascraper.job.controller;

import com.datascraper.common.dto.messaging.DiscoveryQueueMessage;
import com.datascraper.job.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/queue")
public class InternalQueueController {

    private final QueueService queueService;

    public InternalQueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/discovery/poll")
    public ResponseEntity<DiscoveryQueueMessage> pollDiscovery() {
        return queueService.pollDiscovery()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/discovery/size")
    public Map<String, Integer> discoveryQueueSize() {
        return Map.of("size", queueService.discoveryQueueSize());
    }
}
