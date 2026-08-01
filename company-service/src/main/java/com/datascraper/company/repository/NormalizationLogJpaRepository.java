/**
 * Spring Data JPA access for normalization audit logs.
 */
package com.datascraper.company.repository;

import com.datascraper.company.entity.NormalizationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NormalizationLogJpaRepository extends JpaRepository<NormalizationLogEntity, String> {
}
