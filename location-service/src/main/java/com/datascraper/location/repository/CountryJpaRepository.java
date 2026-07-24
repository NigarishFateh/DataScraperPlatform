/**
 * Spring Data JPA access for country entities.
 */
package com.datascraper.location.repository;

import com.datascraper.location.entity.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryJpaRepository extends JpaRepository<CountryEntity, String> {
}
