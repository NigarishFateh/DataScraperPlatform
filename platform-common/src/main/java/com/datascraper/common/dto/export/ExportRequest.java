package com.datascraper.common.dto.export;

import com.datascraper.common.enums.ExportFormat;
import com.datascraper.common.enums.ExportStatus;

import java.time.Instant;
import java.util.UUID;

public record ExportRequest(
        UUID jobId,
        ExportFormat format
) {
    public ExportRequest {
        if (format == null) {
            format = ExportFormat.EXCEL;
        }
    }
}
