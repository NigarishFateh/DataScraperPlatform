/**
 * Spring Data JPA access for category entities.
 */
package com.datascraper.category.repository;

import com.datascraper.category.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, String> {

    List<CategoryEntity> findByIdInOrderByNameAsc(List<String> ids);

    @Query("""
            SELECT c FROM CategoryEntity c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<CategoryEntity> searchByName(@Param("search") String search, Pageable pageable);

    @Query("""
            SELECT c FROM CategoryEntity c
            WHERE c.id IN :ids
              AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<CategoryEntity> searchByIdsAndName(
            @Param("ids") List<String> ids,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT c FROM CategoryEntity c
            WHERE c.id IN :ids
            """)
    Page<CategoryEntity> findByIdIn(@Param("ids") List<String> ids, Pageable pageable);
}
