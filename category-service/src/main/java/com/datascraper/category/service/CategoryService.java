/**
 * Service contract for listing categories.
 */
package com.datascraper.category.service;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.category.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {

    PageResponse<CategoryResponse> searchCategories(
            List<String> ids,
            String search,
            int page,
            int pageSize
    );

    CategoryResponse getDefaultCategory();
}
