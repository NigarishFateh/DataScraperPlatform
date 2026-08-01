/**
 * Spring Data JPA access for validation audit logs.
 */
package com.datascraper.company.repository;

import com.datascraper.company.entity.ValidationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationLogJpaRepository extends JpaRepository<ValidationLogEntity, String> {
}
