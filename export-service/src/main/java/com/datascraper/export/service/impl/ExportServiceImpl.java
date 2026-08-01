package com.datascraper.export.service.impl;

import com.datascraper.common.dto.export.ExportRequest;
import com.datascraper.common.dto.export.ExportResponse;
import com.datascraper.common.enums.ExportFormat;
import com.datascraper.common.enums.ExportStatus;
import com.datascraper.export.entity.ExportHistoryEntity;
import com.datascraper.export.exception.ExportNotFoundException;
import com.datascraper.export.exception.ExportNotReadyException;
import com.datascraper.export.mapper.ExportMapper;
import com.datascraper.export.repository.ExportHistoryRepository;
import com.datascraper.export.service.ExportGenerationService;
import com.datascraper.export.service.ExportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExportServiceImpl implements ExportService {

    private final ExportHistoryRepository repository;
    private final ExportMapper mapper;
    private final ExportGenerationService exportGenerationService;

    public ExportServiceImpl(
            ExportHistoryRepository repository,
            ExportMapper mapper,
            ExportGenerationService exportGenerationService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.exportGenerationService = exportGenerationService;
    }

    @Override
    @Transactional
    public ExportResponse createExport(ExportRequest request) {
        if (request.jobId() == null) {
            throw new IllegalArgumentException("jobId is required");
        }
        ExportFormat format = request.format() != null ? request.format() : ExportFormat.EXCEL;
        if (format != ExportFormat.EXCEL) {
            throw new IllegalArgumentException("Only EXCEL format is currently supported");
        }

        ExportHistoryEntity entity = new ExportHistoryEntity();
        entity.setId(UUID.randomUUID());
        entity.setJobId(request.jobId());
        entity.setFormat(format);
        entity.setStatus(ExportStatus.PENDING);
        entity.setCreatedAt(Instant.now());
        repository.save(entity);

        exportGenerationService.generateExport(entity.getId());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResponse getExport(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportResponse> listByJobId(UUID jobId) {
        return repository.findByJobIdOrderByCreatedAtDesc(jobId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportResponse> listRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return repository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, safeLimit))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadExport(UUID id) {
        ExportHistoryEntity entity = findEntity(id);
        if (entity.getStatus() != ExportStatus.READY) {
            throw new ExportNotReadyException(id, entity.getStatus());
        }
        if (entity.getFilePath() == null || !Files.exists(Path.of(entity.getFilePath()))) {
            throw new ExportNotFoundException(id);
        }
        return new FileSystemResource(entity.getFilePath());
    }

    @Override
    public void generateExportAsync(UUID exportId) {
        exportGenerationService.generateExport(exportId);
    }

    private ExportHistoryEntity findEntity(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ExportNotFoundException(id));
    }
}
