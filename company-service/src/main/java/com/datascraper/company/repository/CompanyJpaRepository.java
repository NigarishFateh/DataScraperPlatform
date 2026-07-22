package com.datascraper.company.repository;

import com.datascraper.company.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanyJpaRepository extends JpaRepository<CompanyEntity, String> {

    @Query("""
            SELECT c FROM CompanyEntity c
            WHERE c.cityId IN :cityIds
              AND (
                    :search = ''
                 OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(c.website) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(c.industry) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY LOWER(c.name) ASC
            """)
    List<CompanyEntity> search(
            @Param("cityIds") List<String> cityIds,
            @Param("search") String search
    );
}
