package com.bankcategorizer.service;

import com.bankcategorizer.model.Category;
import com.bankcategorizer.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Matches a transaction description against configured {@link Category} keywords.
 *
 * <p>A category matches if any of its keywords appears as a case-insensitive substring of
 * the description. When more than one category could match, the first one in the supplied
 * list wins (callers are expected to pass categories in creation order, see
 * {@link CategoryRepository#findAllByOrderByIdAsc()}) — there is no scoring/priority system.
 */
@Service
public class CategorizationService {

    private final CategoryRepository categoryRepository;

    public CategorizationService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Loads the categories to match against, in creation order. Intended to be called once
     * per batch of transactions (e.g. once per file import) rather than once per row.
     */
    public List<Category> loadCategories() {
        return categoryRepository.findAllByOrderByIdAsc();
    }

    /**
     * Returns the first category (in the given order) that has a keyword matching the
     * description, or empty if none match.
     */
    public Optional<Category> match(String description, List<Category> categories) {
        if (description == null || description.isBlank() || categories == null || categories.isEmpty()) {
            return Optional.empty();
        }

        String lowerDescription = description.toLowerCase(Locale.ROOT);
        for (Category category : categories) {
            boolean matches = category.getKeywords().stream()
                    .filter(keyword -> keyword != null && !keyword.isBlank())
                    .anyMatch(keyword -> lowerDescription.contains(keyword.toLowerCase(Locale.ROOT)));
            if (matches) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
