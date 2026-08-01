/**
 * Exposes HTTP endpoints to list categories.
 */
package com.datascraper.category.controller;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.category.dto.CategoryResponse;
import com.datascraper.category.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public PageResponse<CategoryResponse> categories(
            @RequestParam(required = false) List<String> ids,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize
    ) {
        return categoryService.searchCategories(ids, search, page, pageSize);
    }

    @GetMapping("/default")
    public CategoryResponse defaultCategory() {
        return categoryService.getDefaultCategory();
    }
}
