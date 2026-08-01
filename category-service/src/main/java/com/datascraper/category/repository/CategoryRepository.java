/**
 * Loads categories from the database into domain objects.
 */
package com.datascraper.category.repository;

import com.datascraper.category.domain.Category;
import com.datascraper.category.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    public Page<Category> search(List<String> ids, String search, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("name"));
        String q = search == null ? "" : search.trim();
        boolean hasIds = ids != null && !ids.isEmpty();

        Page<CategoryEntity> result;
        if (hasIds && !q.isEmpty()) {
            result = categoryJpaRepository.searchByIdsAndName(ids, q, pageable);
        } else if (hasIds) {
            result = categoryJpaRepository.findByIdIn(ids, pageable);
        } else if (!q.isEmpty()) {
            result = categoryJpaRepository.searchByName(q, pageable);
        } else {
            result = categoryJpaRepository.findAll(pageable);
        }

        return result.map(this::toDomain);
    }

    public List<Category> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return categoryJpaRepository.findByIdInOrderByNameAsc(ids).stream()
                .map(this::toDomain)
                .toList();
    }

    public Optional<Category> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return categoryJpaRepository.findById(id.trim()).map(this::toDomain);
    }

    private Category toDomain(CategoryEntity entity) {
        return new Category(entity.getId(), entity.getName());
    }
}
