package com.bankcategorizer.config;

import com.bankcategorizer.model.Category;
import com.bankcategorizer.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCategorySeedingServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private DefaultCategorySeedingService seedingService;

    @BeforeEach
    void setUp() {
        seedingService = new DefaultCategorySeedingService(categoryRepository);
    }

    @Test
    void seedIfEmpty_repositoryEmpty_savesDefaultCategories() {
        when(categoryRepository.count()).thenReturn(0L);

        seedingService.seedIfEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Category>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(captor.capture());

        List<Category> saved = captor.getValue();
        assertThat(saved).hasSize(DefaultCategorySeedData.DEFAULT_CATEGORIES.size());
        assertThat(saved).extracting(Category::getName)
                .containsExactlyElementsOf(
                        DefaultCategorySeedData.DEFAULT_CATEGORIES.stream()
                                .map(DefaultCategorySeedData.SeedCategory::name)
                                .toList());
        assertThat(saved).allSatisfy(category -> assertThat(category.getKeywords()).isNotEmpty());
    }

    @Test
    void seedIfEmpty_repositoryAlreadyHasCategories_doesNotSeed() {
        when(categoryRepository.count()).thenReturn(3L);

        seedingService.seedIfEmpty();

        verify(categoryRepository, never()).saveAll(org.mockito.ArgumentMatchers.<List<Category>>any());
    }
}
