package com.datascraper.export.service;

import com.datascraper.common.dto.export.ExportRequest;
import com.datascraper.common.dto.export.ExportResponse;
import com.datascraper.common.enums.ExportFormat;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.UUID;

public interface ExportService {

    ExportResponse createExport(ExportRequest request);

    ExportResponse getExport(UUID id);

    List<ExportResponse> listByJobId(UUID jobId);

    List<ExportResponse> listRecent(int limit);

    Resource downloadExport(UUID id);

    void generateExportAsync(UUID exportId);
}
