package com.datascraper.common.dto.export;

import com.datascraper.common.enums.ExportFormat;
import com.datascraper.common.enums.ExportStatus;

import java.time.Instant;
import java.util.UUID;

public record ExportResponse(
        UUID id,
        UUID jobId,
        ExportFormat format,
        ExportStatus status,
        String fileName,
        long rowCount,
        long fileSizeBytes,
        String downloadUrl,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
) {
}
