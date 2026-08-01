/**
 * Spring Data JPA access for provider execution audit logs.
 */
package com.datascraper.company.repository;

import com.datascraper.company.entity.ProviderExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderExecutionJpaRepository extends JpaRepository<ProviderExecutionEntity, String> {
}
