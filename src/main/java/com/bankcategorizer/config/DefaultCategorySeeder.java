package com.bankcategorizer.config;

import com.bankcategorizer.model.Category;
import com.bankcategorizer.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Bootstraps a broad, generic set of default spending categories with well-known
 * merchant/brand keywords (see {@link DefaultCategorySeedData}) so that out-of-the-box
 * auto-categorization (see {@code CategorizationService}) works without the user having to
 * configure anything first.
 *
 * <p>This only ever runs against a completely empty category table: if any category already
 * exists (whether seeded previously or created by the user), seeding is skipped entirely so
 * user customization is never merged into or overwritten.
 */
@Component
public class DefaultCategorySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultCategorySeeder.class);

    private final CategoryRepository categoryRepository;

    public DefaultCategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfEmpty();
    }

    /**
     * Seeds {@link DefaultCategorySeedData#DEFAULT_CATEGORIES} if, and only if, the category
     * table is currently empty. Never merges into or overwrites existing categories.
     */
    void seedIfEmpty() {
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
