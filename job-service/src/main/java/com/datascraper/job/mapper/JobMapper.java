package com.datascraper.job.mapper;

import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.job.entity.ScrapingJobEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobMapper {

    public JobResponse toResponse(ScrapingJobEntity entity) {
        return new JobResponse(
                entity.getId(),
                entity.getStatus(),
                entity.getPhase(),
                entity.getUserId(),
                copyList(entity.getCategoryIds()),
                copyList(entity.getCountryCodes()),
                copyList(entity.getCityIds()),
                entity.getDiscoveredCount(),
                entity.getEnrichedCount(),
                entity.getPersistedCount(),
                entity.getFailedCount(),
                entity.getProgressPercent(),
                entity.getEstimatedRemainingSeconds(),
                entity.getExportId(),
                entity.getErrorMessage(),
                entity.getCheckpoint(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }

    private List<String> copyList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
