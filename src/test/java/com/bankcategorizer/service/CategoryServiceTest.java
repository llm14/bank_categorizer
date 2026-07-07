package com.bankcategorizer.service;

import com.bankcategorizer.dto.CategoryCreateRequest;
import com.bankcategorizer.dto.CategoryResponse;
import com.bankcategorizer.exception.DuplicateCategoryException;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.model.Category;
import com.bankcategorizer.repository.CategoryRepository;
import com.bankcategorizer.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, transactionRepository);
    }

    @Test
    void create_uniqueName_savesAndReturnsResponse() {
        CategoryCreateRequest request = new CategoryCreateRequest("Groceries", "Supermarket spending");
        when(categoryRepository.existsByNameIgnoreCase("Groceries")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category toSave = invocation.getArgument(0);
            toSave.setId(1L);
            return toSave;
        });

        CategoryResponse response = categoryService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Groceries");
        assertThat(response.description()).isEqualTo("Supermarket spending");
    }

    @Test
    void create_duplicateName_throwsDuplicateCategoryException() {
        CategoryCreateRequest request = new CategoryCreateRequest("Groceries", null);
        when(categoryRepository.existsByNameIgnoreCase("Groceries")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(DuplicateCategoryException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void findAll_returnsMappedResponses() {
        Category groceries = Category.builder().id(1L).name("Groceries").description(null).build();
        Category rent = Category.builder().id(2L).name("Rent").description("Monthly rent").build();
        when(categoryRepository.findAll()).thenReturn(List.of(groceries, rent));

        List<CategoryResponse> responses = categoryService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CategoryResponse::name).containsExactly("Groceries", "Rent");
    }

    @Test
    void delete_existingCategory_uncategorizesTransactionsBeforeDeletingCategory() {
        Category category = Category.builder().id(5L).name("Transport").build();
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        categoryService.delete(5L);

        InOrder inOrder = inOrder(transactionRepository, categoryRepository);
        inOrder.verify(transactionRepository).uncategorizeByCategoryId(5L);
        inOrder.verify(categoryRepository).delete(category);
    }

    @Test
    void delete_missingCategory_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).uncategorizeByCategoryId(anyLong());
        verify(categoryRepository, never()).delete(any());
    }
}
