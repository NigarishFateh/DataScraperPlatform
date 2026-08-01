/**
 * Spring Data JPA access for country entities.
 */
package com.datascraper.location.repository;

import com.datascraper.location.entity.CountryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CountryJpaRepository extends JpaRepository<CountryEntity, String> {

    @Query("""
            SELECT c FROM CountryEntity c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<CountryEntity> searchByNameOrCode(@Param("search") String search, Pageable pageable);
}
