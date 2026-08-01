package com.datascraper.discovery.service;

import com.datascraper.common.enums.ProviderExecutionStatus;
import com.datascraper.common.provider.DiscoveryProvider;
import com.datascraper.discovery.entity.DiscoveryLogEntity;
import com.datascraper.discovery.repository.DiscoveryLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DiscoveryLogService {

    private final DiscoveryLogRepository discoveryLogRepository;

    public DiscoveryLogService(DiscoveryLogRepository discoveryLogRepository) {
        this.discoveryLogRepository = discoveryLogRepository;
    }

    @Transactional
    public void saveLogs(UUID jobId, List<ProviderExecutionRecord> records) {
        List<DiscoveryLogEntity> entities = new ArrayList<>();
        Instant now = Instant.now();

        for (ProviderExecutionRecord record : records) {
            DiscoveryLogEntity entity = new DiscoveryLogEntity();
            entity.setId(UUID.randomUUID());
            entity.setJobId(jobId);
            entity.setProviderName(record.provider().name());
            entity.setProviderType(record.provider().type().name());
            entity.setRequestSummary(record.requestSummary());
            entity.setResultCount(record.resultCount());
            entity.setStatus(record.status().name());
            entity.setMessage(record.message());
            entity.setCreatedAt(now);
            entities.add(entity);
        }

        discoveryLogRepository.saveAll(entities);
    }

    public record ProviderExecutionRecord(
            DiscoveryProvider provider,
            String requestSummary,
            int resultCount,
            ProviderExecutionStatus status,
            String message
    ) {
    }
}
