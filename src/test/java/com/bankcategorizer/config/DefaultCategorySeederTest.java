package com.bankcategorizer.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Verifies {@link DefaultCategorySeeder} delegates to {@link DefaultCategorySeedingService}
 * (a separate Spring bean) rather than performing the check-then-act seeding itself, so the
 * seeding service's {@code @Transactional} is applied via the Spring proxy rather than being
 * bypassed by self-invocation.
 */
@ExtendWith(MockitoExtension.class)
class DefaultCategorySeederTest {

    @Mock
    private DefaultCategorySeedingService seedingService;

    private DefaultCategorySeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DefaultCategorySeeder(seedingService);
    }

    @Test
    void run_delegatesToSeedingService() {
        seeder.run(null);

        verify(seedingService).seedIfEmpty();
    }
}
