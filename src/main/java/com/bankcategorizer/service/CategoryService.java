package com.bankcategorizer.service;

import com.bankcategorizer.dto.CategoryCreateRequest;
import com.bankcategorizer.dto.CategoryResponse;
import com.bankcategorizer.exception.DuplicateCategoryException;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.model.Category;
import com.bankcategorizer.repository.CategoryRepository;
import com.bankcategorizer.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for managing spending categories: creation with a uniqueness check,
 * listing, and deletion. Deleting a category uncategorizes (does not delete) any
 * transactions that reference it.
 */
@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateCategoryException(
                    "A category named '%s' already exists".formatted(request.name()));
        }

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .keywords(normalizeKeywords(request.keywords()))
                .build();
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category %d not found".formatted(id)));

        transactionRepository.uncategorizeByCategoryId(category.getId());
        categoryRepository.delete(category);

        log.info("Deleted category {} ('{}') and uncategorized its transactions", id, category.getName());
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(),
                List.copyOf(category.getKeywords()));
    }

    private Set<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) {
            return new LinkedHashSet<>();
        }
        return keywords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
