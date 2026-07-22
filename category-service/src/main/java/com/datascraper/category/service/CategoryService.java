package com.datascraper.category.service;

import com.datascraper.category.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> listCategories();

    List<CategoryResponse> listByIds(List<String> ids);
}
