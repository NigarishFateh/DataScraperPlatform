package com.datascraper.export.entity;

import com.datascraper.common.enums.ExportFormat;
import com.datascraper.common.enums.ExportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "export_history")
@Getter
@Setter
public class ExportHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExportStatus status;

    @Column(name = "file_name", length = 512)
    private String fileName;

    @Column(name = "file_path", length = 1024)
    private String filePath;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "search_criteria", columnDefinition = "TEXT")
    private String searchCriteria;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
