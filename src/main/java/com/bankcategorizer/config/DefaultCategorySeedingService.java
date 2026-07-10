package com.bankcategorizer.config;

import com.bankcategorizer.model.Category;
import com.bankcategorizer.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Holds the actual check-then-act seeding logic for {@link DefaultCategorySeeder}, as its own
 * Spring-managed bean so that {@link #seedIfEmpty()}'s {@code @Transactional} is applied via the
 * Spring AOP proxy. {@link DefaultCategorySeeder} (an {@code ApplicationRunner}) calls this bean
 * from outside, rather than invoking a transactional method on itself, which would otherwise
 * bypass the proxy (self-invocation) and silently make {@code @Transactional} a no-op.
 */
@Service
public class DefaultCategorySeedingService {

    private static final Logger log = LoggerFactory.getLogger(DefaultCategorySeedingService.class);

    private final CategoryRepository categoryRepository;

    public DefaultCategorySeedingService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Seeds {@link DefaultCategorySeedData#DEFAULT_CATEGORIES} if, and only if, the category
     * table is currently empty. Never merges into or overwrites existing categories.
     */
    @Transactional
    public void seedIfEmpty() {
        if (categoryRepository.count() > 0) {
            log.debug("Categories already exist; skipping default category seeding");
            return;
        }

        List<Category> categories = DefaultCategorySeedData.DEFAULT_CATEGORIES.stream()
                .map(seed -> Category.builder()
                        .name(seed.name())
                        .description(seed.description())
                        .keywords(new LinkedHashSet<>(seed.keywords()))
                        .build())
                .toList();

        categoryRepository.saveAll(categories);
        log.info("Seeded {} default categories with built-in merchant keywords", categories.size());
    }
}
