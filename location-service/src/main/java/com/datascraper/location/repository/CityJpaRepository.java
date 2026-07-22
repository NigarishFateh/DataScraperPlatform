package com.datascraper.location.repository;

import com.datascraper.location.entity.CityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CityJpaRepository extends JpaRepository<CityEntity, String> {

    List<CityEntity> findByCountryCodeOrderByNameAsc(String countryCode);

    @Query("""
            SELECT c FROM CityEntity c
            WHERE c.countryCode = :countryCode
              AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY c.name ASC
            """)
    List<CityEntity> searchByCountryAndName(
            @Param("countryCode") String countryCode,
            @Param("search") String search
    );
}
