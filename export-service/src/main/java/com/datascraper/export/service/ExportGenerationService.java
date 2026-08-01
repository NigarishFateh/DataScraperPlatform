package com.datascraper.export.service;

import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.export.client.CompanyServiceClient;
import com.datascraper.export.client.JobServiceClient;
import com.datascraper.export.config.ExportProperties;
import com.datascraper.export.entity.ExportHistoryEntity;
import com.datascraper.export.exception.ExportNotFoundException;
import com.datascraper.export.repository.ExportHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExportGenerationService {

    private final ExportHistoryRepository repository;
    private final ExportProperties properties;
    private final CompanyServiceClient companyServiceClient;
    private final JobServiceClient jobServiceClient;
    private final ExcelExportWriter excelExportWriter;
    private final ObjectMapper objectMapper;

    public ExportGenerationService(
            ExportHistoryRepository repository,
            ExportProperties properties,
            CompanyServiceClient companyServiceClient,
            JobServiceClient jobServiceClient,
            ExcelExportWriter excelExportWriter,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.properties = properties;
        this.companyServiceClient = companyServiceClient;
        this.jobServiceClient = jobServiceClient;
        this.excelExportWriter = excelExportWriter;
        this.objectMapper = objectMapper;
    }

    @Async("exportTaskExecutor")
    @Transactional
    public void generateExport(UUID exportId) {
        ExportHistoryEntity entity = repository.findById(exportId)
                .orElseThrow(() -> new ExportNotFoundException(exportId));

        entity.setStatus(com.datascraper.common.enums.ExportStatus.GENERATING);
        repository.save(entity);

        try {
            JobResponse job = jobServiceClient.fetchJob(entity.getJobId());
            entity.setSearchCriteria(objectMapper.writeValueAsString(job));
            repository.save(entity);

            List<EnrichedCompany> companies = companyServiceClient.fetchCompaniesByJob(entity.getJobId());
            Instant generatedAt = Instant.now();
            Path storageDir = Path.of(properties.getStoragePath()).toAbsolutePath().normalize();
            Files.createDirectories(storageDir);

            String fileName = "export-" + entity.getJobId() + "-" + exportId + ".xlsx";
            Path outputFile = storageDir.resolve(fileName);

            ExcelExportWriter.ExportWriteResult result = excelExportWriter.writeWorkbook(
                    outputFile,
                    companies,
                    job,
                    properties.getAppVersion(),
                    generatedAt
            );

            entity.setFileName(fileName);
            entity.setFilePath(outputFile.toString());
            entity.setRowCount(result.rowCount());
            entity.setFileSizeBytes(result.fileSizeBytes());
            entity.setStatus(com.datascraper.common.enums.ExportStatus.READY);
            entity.setCompletedAt(Instant.now());
            entity.setErrorMessage(null);
            repository.save(entity);

            jobServiceClient.completeJob(entity.getJobId(), exportId);
        } catch (Exception ex) {
            entity.setStatus(com.datascraper.common.enums.ExportStatus.FAILED);
            entity.setErrorMessage(ex.getMessage());
            entity.setCompletedAt(Instant.now());
            repository.save(entity);
        }
    }
}
