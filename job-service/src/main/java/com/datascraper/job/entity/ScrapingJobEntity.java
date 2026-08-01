package com.datascraper.job.entity;

import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;
import com.datascraper.job.util.JsonListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scraping_jobs")
@Getter
@Setter
public class ScrapingJobEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobPhase phase;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "category_ids", nullable = false, columnDefinition = "TEXT")
    private List<String> categoryIds = new ArrayList<>();

    @Convert(converter = JsonListConverter.class)
    @Column(name = "country_codes", nullable = false, columnDefinition = "TEXT")
    private List<String> countryCodes = new ArrayList<>();

    @Convert(converter = JsonListConverter.class)
    @Column(name = "city_ids", nullable = false, columnDefinition = "TEXT")
    private List<String> cityIds = new ArrayList<>();

    @Convert(converter = JsonListConverter.class)
    @Column(name = "enabled_providers", nullable = false, columnDefinition = "TEXT")
    private List<String> enabledProviders = new ArrayList<>();

    @Column(name = "max_companies", nullable = false)
    private int maxCompanies = 200;

    @Column(name = "discovered_count", nullable = false)
    private int discoveredCount;

    @Column(name = "enriched_count", nullable = false)
    private int enrichedCount;

    @Column(name = "persisted_count", nullable = false)
    private int persistedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "estimated_remaining_seconds")
    private Long estimatedRemainingSeconds;

    @Column(name = "export_id", length = 128)
    private String exportId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String checkpoint;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
