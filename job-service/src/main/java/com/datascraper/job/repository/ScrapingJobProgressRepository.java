package com.datascraper.job.repository;

import com.datascraper.job.entity.ScrapingJobProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScrapingJobProgressRepository extends JpaRepository<ScrapingJobProgressEntity, UUID> {
}
