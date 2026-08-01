package com.datascraper.export.repository;

import com.datascraper.export.entity.ExportHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExportHistoryRepository extends JpaRepository<ExportHistoryEntity, UUID> {

    List<ExportHistoryEntity> findByJobIdOrderByCreatedAtDesc(UUID jobId);

    List<ExportHistoryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
