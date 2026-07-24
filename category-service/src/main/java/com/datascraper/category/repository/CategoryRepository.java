/**
 * Loads categories from the database into domain objects.
 */
package com.datascraper.category.repository;

import com.datascraper.category.domain.Category;
import com.datascraper.category.entity.CategoryEntity;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    public CategoryRepository(CategoryJpaRepository categoryJpaRepository) {
        this.categoryJpaRepository = categoryJpaRepository;
    }

    public List<Category> findAll() {
        return categoryJpaRepository.findAll(Sort.by("name")).stream()
                .map(this::toDomain)
                .toList();
    }

    public List<Category> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return categoryJpaRepository.findByIdInOrderByNameAsc(ids).stream()
                .map(this::toDomain)
                .toList();
    }

    private Category toDomain(CategoryEntity entity) {
        return new Category(entity.getId(), entity.getName());
    }
}
