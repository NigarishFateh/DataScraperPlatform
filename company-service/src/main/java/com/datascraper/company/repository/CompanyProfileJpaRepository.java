/**
 * Spring Data JPA access for enriched company profiles.
 */
package com.datascraper.company.repository;

import com.datascraper.company.entity.CompanyProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyProfileJpaRepository extends JpaRepository<CompanyProfileEntity, String> {

    Optional<CompanyProfileEntity> findByJobIdAndNormalizedKey(UUID jobId, String normalizedKey);

    Page<CompanyProfileEntity> findByJobId(UUID jobId, Pageable pageable);
}
