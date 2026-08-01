package com.datascraper.export.mapper;

import com.datascraper.common.dto.export.ExportResponse;
import com.datascraper.export.entity.ExportHistoryEntity;
import org.springframework.stereotype.Component;

@Component
public class ExportMapper {

    public ExportResponse toResponse(ExportHistoryEntity entity) {
        String downloadUrl = entity.getFilePath() != null
                ? "/api/exports/" + entity.getId() + "/download"
                : null;
        return new ExportResponse(
                entity.getId(),
                entity.getJobId(),
                entity.getFormat(),
                entity.getStatus(),
                entity.getFileName(),
                entity.getRowCount() != null ? entity.getRowCount() : 0L,
                entity.getFileSizeBytes() != null ? entity.getFileSizeBytes() : 0L,
                downloadUrl,
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}
