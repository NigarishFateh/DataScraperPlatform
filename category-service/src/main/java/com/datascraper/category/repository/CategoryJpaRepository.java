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

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, String> {

    @Query("""
            SELECT c FROM CategoryEntity c
            WHERE c.id IN :ids
            ORDER BY CASE WHEN c.id = 'cleaning' THEN 0 ELSE 1 END, c.name ASC
            """)
    List<CategoryEntity> findByIdInOrderByNameAsc(@Param("ids") List<String> ids);

    @Query("""
            SELECT c FROM CategoryEntity c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY CASE WHEN c.id = 'cleaning' THEN 0 ELSE 1 END, c.name ASC
            """)
    Page<CategoryEntity> searchByName(@Param("search") String search, Pageable pageable);

    @Query("""
            SELECT c FROM CategoryEntity c
            WHERE c.id IN :ids
              AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY CASE WHEN c.id = 'cleaning' THEN 0 ELSE 1 END, c.name ASC
            """)
    Page<CategoryEntity> searchByIdsAndName(
            @Param("ids") List<String> ids,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT c FROM CategoryEntity c
            WHERE c.id IN :ids
            ORDER BY CASE WHEN c.id = 'cleaning' THEN 0 ELSE 1 END, c.name ASC
            """)
    Page<CategoryEntity> findByIdIn(@Param("ids") List<String> ids, Pageable pageable);

    @Query("""
            SELECT c FROM CategoryEntity c
            ORDER BY CASE WHEN c.id = 'cleaning' THEN 0 ELSE 1 END, c.name ASC
            """)
    Page<CategoryEntity> findAllOrdered(Pageable pageable);
}
