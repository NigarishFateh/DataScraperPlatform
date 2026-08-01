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
    private final ConcurrentHashMap<UUID, AtomicInteger> localPersistedCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicInteger> localFailedCounts = new ConcurrentHashMap<>();
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
        return increment(jobId, "enriched", localEnrichedCounts);
    }

    public int incrementPersisted(UUID jobId) {
        return increment(jobId, "persisted", localPersistedCounts);
    }

    public int incrementFailed(UUID jobId) {
        return increment(jobId, "failed", localFailedCounts);
    }

    public int currentPersistedCount(UUID jobId) {
        return current(jobId, "persisted", localPersistedCounts);
    }

    public int currentFailedCount(UUID jobId) {
        return current(jobId, "failed", localFailedCounts);
    }

    public void checkAndTriggerExport(UUID jobId, int enrichedCount) {
        if (exportTriggered.putIfAbsent(jobId, Boolean.TRUE) != null) {
            return;
        }

        JobResponse job = jobServiceClient.getJob(jobId);
        int discovered = job != null ? job.discoveredCount() : enrichedCount;
        if (discovered <= 0 || enrichedCount < discovered) {
            exportTriggered.remove(jobId);
            return;
        }

        log.info("Job {} enrichment complete ({}/{}), triggering export", jobId, enrichedCount, discovered);
        try {
            exportTriggerClient.triggerExport(jobId);
        } catch (Exception ex) {
            log.warn("Export trigger failed for job {}: {}", jobId, ex.getMessage());
            exportTriggered.remove(jobId);
            jobServiceClient.completeJob(jobId, null);
        }
    }

    public int currentEnrichedCount(UUID jobId, JobResponse job) {
        int local = current(jobId, "enriched", localEnrichedCounts);
        if (local > 0) {
            return local;
        }
        if (job != null && job.enrichedCount() > 0) {
            return job.enrichedCount();
        }
        return 0;
    }

    private int increment(UUID jobId, String kind, ConcurrentHashMap<UUID, AtomicInteger> localMap) {
        if (properties.getRedis().isEnabled() && redisTemplate != null) {
            Long count = redisTemplate.opsForValue().increment(counterKey(jobId, kind));
            return count != null ? count.intValue() : 1;
        }
        return localMap.computeIfAbsent(jobId, ignored -> new AtomicInteger(0)).incrementAndGet();
    }

    private int current(UUID jobId, String kind, ConcurrentHashMap<UUID, AtomicInteger> localMap) {
        if (properties.getRedis().isEnabled() && redisTemplate != null) {
            String value = redisTemplate.opsForValue().get(counterKey(jobId, kind));
            if (value != null) {
                return Integer.parseInt(value);
            }
        }
        AtomicInteger local = localMap.get(jobId);
        return local != null ? local.get() : 0;
    }

    private static String counterKey(UUID jobId, String kind) {
        return PlatformQueues.COMPANY_ENRICHMENT + ":count:" + kind + ":" + jobId;
    }
}
