package com.datascraper.orchestrator.job;

import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.common.queue.PlatformQueues;
import com.datascraper.orchestrator.client.ExportTriggerClient;
import com.datascraper.orchestrator.client.JobServiceClient;
import com.datascraper.orchestrator.config.OrchestratorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class JobCompletionTracker {

    private final JobServiceClient jobServiceClient;
    private final ExportTriggerClient exportTriggerClient;
    private final OrchestratorProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<UUID, AtomicInteger> localEnrichedCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> exportTriggered = new ConcurrentHashMap<>();

    public JobCompletionTracker(
            JobServiceClient jobServiceClient,
            ExportTriggerClient exportTriggerClient,
            OrchestratorProperties properties,
            @Autowired(required = false) StringRedisTemplate redisTemplate
    ) {
        this.jobServiceClient = jobServiceClient;
        this.exportTriggerClient = exportTriggerClient;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    public int incrementEnriched(UUID jobId) {
        if (properties.getRedis().isEnabled() && redisTemplate != null) {
            Long count = redisTemplate.opsForValue().increment(enrichedCounterKey(jobId));
            return count != null ? count.intValue() : 1;
        }
        return localEnrichedCounts.computeIfAbsent(jobId, ignored -> new AtomicInteger(0)).incrementAndGet();
    }

    public void checkAndTriggerExport(UUID jobId, int enrichedCount) {
        if (exportTriggered.putIfAbsent(jobId, Boolean.TRUE) != null) {
            return;
        }

        JobResponse job = jobServiceClient.getJob(jobId);
        int discovered = job != null ? job.discoveredCount() : enrichedCount;
        if (discovered <= 0) {
            exportTriggered.remove(jobId);
            return;
        }

        if (enrichedCount < discovered) {
            exportTriggered.remove(jobId);
            return;
        }

        log.info("Job {} enrichment complete ({}/{}), triggering export", jobId, enrichedCount, discovered);
        exportTriggerClient.triggerExport(jobId);
    }

    public int currentEnrichedCount(UUID jobId, JobResponse job) {
        if (job != null && job.enrichedCount() > 0) {
            return job.enrichedCount();
        }
        if (properties.getRedis().isEnabled() && redisTemplate != null) {
            String value = redisTemplate.opsForValue().get(enrichedCounterKey(jobId));
            if (value != null) {
                return Integer.parseInt(value);
            }
        }
        AtomicInteger local = localEnrichedCounts.get(jobId);
        return local != null ? local.get() : 0;
    }

    private static String enrichedCounterKey(UUID jobId) {
        return PlatformQueues.COMPANY_ENRICHMENT + ":count:" + jobId;
    }
}
