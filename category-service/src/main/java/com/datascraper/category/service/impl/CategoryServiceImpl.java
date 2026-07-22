package com.datascraper.category.service.impl;

import com.datascraper.category.dto.CategoryResponse;
import com.datascraper.category.repository.CategoryRepository;
import com.datascraper.category.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> new CategoryResponse(category.id(), category.name()))
                .toList();
    }

    @Override
    public List<CategoryResponse> listByIds(List<String> ids) {
        return categoryRepository.findByIds(ids).stream()
                .map(category -> new CategoryResponse(category.id(), category.name()))
                .toList();
    }
}
