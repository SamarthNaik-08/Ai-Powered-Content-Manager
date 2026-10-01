package com.contentmanager.modules.content.service;

import com.contentmanager.common.exception.DuplicateResourceException;
import com.contentmanager.common.exception.ResourceNotFoundException;
import com.contentmanager.modules.content.dto.*;
import com.contentmanager.modules.content.model.Category;
import com.contentmanager.modules.content.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Tests")
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private CategoryService categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Technology")
                .slug("technology")
                .description("Tech articles")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getAllCategories returns all categories")
    void getAllCategories_success() {
        when(categoryRepository.findAll()).thenReturn(List.of(testCategory));

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Technology");
    }

    @Test
    @DisplayName("createCategory creates a category with auto slug")
    void createCategory_success() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Design")
                .description("Design articles")
                .build();

        when(categoryRepository.existsByName("Design")).thenReturn(false);
        when(categoryRepository.existsBySlug("design")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(2L);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response.getName()).isEqualTo("Design");
        assertThat(response.getSlug()).isEqualTo("design");
    }

    @Test
    @DisplayName("createCategory throws on duplicate name")
    void createCategory_duplicate() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Technology")
                .build();

        when(categoryRepository.existsByName("Technology")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("getCategoryById returns category")
    void getCategoryById_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        CategoryResponse response = categoryService.getCategoryById(1L);

        assertThat(response.getName()).isEqualTo("Technology");
    }

    @Test
    @DisplayName("getCategoryById throws for non-existent id")
    void getCategoryById_notFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteCategory removes category")
    void deleteCategory_success() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("updateCategory updates name and slug")
    void updateCategory_success() {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("New Tech")
                .description("Updated desc")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByName("New Tech")).thenReturn(false);
        when(categoryRepository.existsBySlug("new-tech")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.updateCategory(1L, request);

        assertThat(response.getName()).isEqualTo("New Tech");
    }
}
