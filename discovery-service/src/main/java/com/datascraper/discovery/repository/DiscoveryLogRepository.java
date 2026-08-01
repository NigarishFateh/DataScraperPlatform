package com.datascraper.discovery.repository;

import com.datascraper.discovery.entity.DiscoveryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscoveryLogRepository extends JpaRepository<DiscoveryLogEntity, UUID> {

    List<DiscoveryLogEntity> findByJobIdOrderByCreatedAtAsc(UUID jobId);
}
