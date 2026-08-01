package com.datascraper.discovery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "discovery_logs")
@Getter
@Setter
@NoArgsConstructor
public class DiscoveryLogEntity {

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "provider_name", nullable = false, length = 128)
    private String providerName;

    @Column(name = "provider_type", nullable = false, length = 64)
    private String providerType;

    @Column(name = "request_summary")
    private String requestSummary;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(nullable = false, length = 32)
    private String status;

    @Column
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
