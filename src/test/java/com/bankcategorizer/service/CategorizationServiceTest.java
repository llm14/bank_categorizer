package com.bankcategorizer.service;

import com.bankcategorizer.model.Category;
import com.bankcategorizer.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorizationServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategorizationService categorizationService;

    @BeforeEach
    void setUp() {
        categorizationService = new CategorizationService(categoryRepository);
    }

    @Test
    void loadCategories_delegatesToRepositoryOrderedById() {
        Category groceries = category(1L, "Groceries", "supermarket");
        when(categoryRepository.findAllByOrderByIdAsc()).thenReturn(List.of(groceries));

        List<Category> result = categorizationService.loadCategories();

        assertThat(result).containsExactly(groceries);
    }

    @Test
    void match_keywordSubstringPresent_returnsMatchingCategory() {
        Category groceries = category(1L, "Groceries", "supermarket");

        Optional<Category> result = categorizationService.match("Payment to Local Supermarket", List.of(groceries));

        assertThat(result).contains(groceries);
    }

    @Test
    void match_isCaseInsensitive() {
        Category groceries = category(1L, "Groceries", "SUPERMARKET");

        Optional<Category> result = categorizationService.match("payment to local supermarket", List.of(groceries));

        assertThat(result).contains(groceries);
    }

    @Test
    void match_noKeywordMatches_returnsEmpty() {
        Category groceries = category(1L, "Groceries", "supermarket");

        Optional<Category> result = categorizationService.match("Monthly Salary", List.of(groceries));

        assertThat(result).isEmpty();
    }

    @Test
    void match_multipleCategoriesMatch_firstInListWins() {
        Category groceries = category(1L, "Groceries", "shop");
        Category dining = category(2L, "Dining", "shop");

        Optional<Category> result = categorizationService.match("Coffee Shop", List.of(groceries, dining));

        assertThat(result).contains(groceries);
    }

    @Test
    void match_blankDescription_returnsEmpty() {
        Category groceries = category(1L, "Groceries", "supermarket");

        assertThat(categorizationService.match("", List.of(groceries))).isEmpty();
        assertThat(categorizationService.match(null, List.of(groceries))).isEmpty();
    }

    @Test
    void match_noCategoriesConfigured_returnsEmpty() {
        assertThat(categorizationService.match("Anything", List.of())).isEmpty();
    }

    @Test
    void match_categoryWithBlankKeywords_isIgnoredSafely() {
        Category noKeywords = category(1L, "Misc");
        noKeywords.setKeywords(new LinkedHashSet<>(List.of("", "   ")));

        assertThat(categorizationService.match("Anything at all", List.of(noKeywords))).isEmpty();
    }

    private Category category(Long id, String name, String... keywords) {
        Set<String> keywordSet = new LinkedHashSet<>(List.of(keywords));
        return Category.builder().id(id).name(name).keywords(keywordSet).build();
    }
}
