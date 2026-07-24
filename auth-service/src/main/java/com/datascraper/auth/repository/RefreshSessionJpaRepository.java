/**
 * Spring Data JPA access for refresh session entities.
 */
package com.datascraper.auth.repository;

import com.datascraper.auth.entity.RefreshSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshSessionJpaRepository extends JpaRepository<RefreshSessionEntity, String> {

    Optional<RefreshSessionEntity> findByToken(String token);

    void deleteByToken(String token);
}
