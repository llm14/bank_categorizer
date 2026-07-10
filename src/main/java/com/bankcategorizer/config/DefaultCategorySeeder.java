package com.bankcategorizer.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Bootstraps a broad, generic set of default spending categories with well-known
 * merchant/brand keywords (see {@link DefaultCategorySeedData}) so that out-of-the-box
 * auto-categorization (see {@code CategorizationService}) works without the user having to
 * configure anything first.
 *
 * <p>This only ever runs against a completely empty category table: if any category already
 * exists (whether seeded previously or created by the user), seeding is skipped entirely so
 * user customization is never merged into or overwritten.
 *
 * <p>The actual check-then-act logic lives in {@link DefaultCategorySeedingService}, a separate
 * Spring bean, so that its {@code @Transactional} is applied via the proxy when this runner
 * calls it (self-invocation from within this class would otherwise bypass the proxy and make
 * {@code @Transactional} a no-op).
 */
@Component
public class DefaultCategorySeeder implements ApplicationRunner {

    private final DefaultCategorySeedingService seedingService;

    public DefaultCategorySeeder(DefaultCategorySeedingService seedingService) {
        this.seedingService = seedingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedingService.seedIfEmpty();
    }
}
