package com.datascraper.job.repository;

import com.datascraper.job.entity.ScrapingJobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScrapingJobRepository extends JpaRepository<ScrapingJobEntity, UUID> {

    Page<ScrapingJobEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
