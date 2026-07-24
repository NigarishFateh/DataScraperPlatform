/**
 * Spring Data JPA access for category entities.
 */
package com.datascraper.category.repository;

import com.datascraper.category.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, String> {

    List<CategoryEntity> findByIdInOrderByNameAsc(List<String> ids);
}
