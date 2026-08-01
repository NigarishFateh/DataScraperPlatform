package com.datascraper.discovery.controller;

import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.common.dto.messaging.DiscoveryQueueMessage;
import com.datascraper.discovery.dto.DiscoveryRunResponse;
import com.datascraper.discovery.dto.ProviderInfoResponse;
import com.datascraper.discovery.factory.DiscoveryProviderFactory;
import com.datascraper.discovery.service.DiscoveryOrchestrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final DiscoveryProviderFactory providerFactory;
    private final DiscoveryOrchestrationService orchestrationService;

    public DiscoveryController(
            DiscoveryProviderFactory providerFactory,
            DiscoveryOrchestrationService orchestrationService
    ) {
        this.providerFactory = providerFactory;
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/run")
    public DiscoveryRunResponse run(
            @Valid @RequestBody DiscoveryRequest request,
            @RequestParam(required = false) List<String> enabledProviders
    ) {
        return orchestrationService.runSync(request, enabledProviders);
    }

    @PostMapping("/consume")
    public DiscoveryRunResponse consume(@RequestBody DiscoveryQueueMessage message) {
        return orchestrationService.processQueueMessage(message);
    }

    @GetMapping("/providers")
    public List<ProviderInfoResponse> providers() {
        return providerFactory.listProviders();
    }
}
