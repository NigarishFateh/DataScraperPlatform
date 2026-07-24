/**
 * Spring Data JPA access and search queries for company entities.
 */
package com.datascraper.company.repository;

import com.datascraper.company.entity.CompanyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanyJpaRepository extends JpaRepository<CompanyEntity, String> {

    @Query(
            value = """
                    SELECT c FROM CompanyEntity c
                    WHERE c.cityId IN :cityIds
                      AND (
                            :search = ''
                         OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(c.website) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(c.industry) LIKE LOWER(CONCAT('%', :search, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(c) FROM CompanyEntity c
                    WHERE c.cityId IN :cityIds
                      AND (
                            :search = ''
                         OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(c.website) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(c.industry) LIKE LOWER(CONCAT('%', :search, '%'))
                      )
                    """
    )
    Page<CompanyEntity> searchByCities(
            @Param("cityIds") List<String> cityIds,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT DISTINCT c FROM CompanyEntity c
                    JOIN c.categoryIds catFilter
                    WHERE c.cityId IN :cityIds
                      AND (
                            :search = ''
                         OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(c.website) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(c.industry) LIKE LOWER(CONCAT('%', :search, '%'))
                      )
                      AND catFilter IN :categoryIds
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT c) FROM CompanyEntity c
                    JOIN c.categoryIds catFilter
                    WHERE c.cityId IN :cityIds
                      AND (
                            :search = ''
                         OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(c.website) LIKE LOWER(CONCAT('%', :search, '%'))
                         OR LOWER(c.industry) LIKE LOWER(CONCAT('%', :search, '%'))
                      )
                      AND catFilter IN :categoryIds
                    """
    )
    Page<CompanyEntity> searchByCitiesAndCategories(
            @Param("cityIds") List<String> cityIds,
            @Param("search") String search,
            @Param("categoryIds") List<String> categoryIds,
            Pageable pageable
    );
}
