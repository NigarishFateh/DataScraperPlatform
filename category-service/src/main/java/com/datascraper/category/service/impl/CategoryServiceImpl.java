/**
 * Implements category listing by fetching from the repository.
 */
package com.datascraper.category.service.impl;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.category.domain.Category;
import com.datascraper.category.dto.CategoryResponse;
import com.datascraper.category.exception.CategoryNotFoundException;
import com.datascraper.category.repository.CategoryRepository;
import com.datascraper.category.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    static final String DEFAULT_CATEGORY_ID = "cleaning";

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public PageResponse<CategoryResponse> searchCategories(
            List<String> ids,
            String search,
            int page,
            int pageSize
    ) {
        int safePage = Math.max(page, 0);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<Category> result =
                categoryRepository.search(ids, search, safePage, safePageSize);
        List<CategoryResponse> items = result.getContent().stream()
                .map(category -> new CategoryResponse(category.id(), category.name()))
                .toList();
        return PageResponse.of(items, safePage, safePageSize, result.getTotalElements());
    }

    @Override
    public CategoryResponse getDefaultCategory() {
        return categoryRepository.findById(DEFAULT_CATEGORY_ID)
                .map(category -> new CategoryResponse(category.id(), category.name()))
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Default category not found: " + DEFAULT_CATEGORY_ID));
    }
}
