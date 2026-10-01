package com.contentmanager.modules.content.service;

import com.contentmanager.common.exception.DuplicateResourceException;
import com.contentmanager.common.exception.ResourceNotFoundException;
import com.contentmanager.modules.content.dto.*;
import com.contentmanager.modules.content.model.Category;
import com.contentmanager.modules.content.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException("Category with name '" + name + "' already exists.");
        }

        String slug = generateSlug(name);
        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: '{}'", saved.getName());
        return mapToResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            String name = request.getName().trim();
            if (!name.equals(category.getName()) && categoryRepository.existsByName(name)) {
                throw new DuplicateResourceException("Category with name '" + name + "' already exists.");
            }
            category.setName(name);
            category.setSlug(generateSlug(name));
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        Category saved = categoryRepository.save(category);
        log.info("Category updated: id={}", id);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
        log.info("Category deleted: id={}", id);
    }

    private String generateSlug(String name) {
        String base = name.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        String slug = base;
        int counter = 1;
        while (categoryRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
