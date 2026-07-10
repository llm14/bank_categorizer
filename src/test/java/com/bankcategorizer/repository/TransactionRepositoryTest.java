package com.bankcategorizer.repository;

import com.bankcategorizer.model.Category;
import com.bankcategorizer.model.Transaction;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@code @DataJpaTest} slice test for {@link TransactionRepository}, run against a real,
 * disposable PostgreSQL instance via Testcontainers (same pattern as
 * {@code ImportCategorizeSpendIntegrationTest}) rather than the {@code @DataJpaTest} default of
 * an in-memory H2 database: the Flyway migrations use Postgres-specific syntax
 * (GENERATED ... AS IDENTITY, setval(...), ALTER COLUMN ... DROP IDENTITY), so H2 would either
 * fail to apply them or mask the very dialect-specific fetch behavior this test is meant to
 * guard.
 *
 * <p>Regression guard for the {@code @EntityGraph(attributePaths = "category")} on
 * {@link TransactionRepository#findAll} and {@link TransactionRepository#findByCategoryIsNull}:
 * the existing Mockito-based service tests can't detect a missing/broken {@code @EntityGraph}
 * because a mocked repository always returns fully-materialized objects regardless of fetch
 * strategy.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class TransactionRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        Category category = Category.builder()
                .name("Groceries")
                .description("Supermarkets and grocery stores")
                .build();
        entityManager.persist(category);
        categoryId = category.getId();

        Transaction categorized = Transaction.builder()
                .date(LocalDate.of(2026, 1, 15))
                .description("MERCADONA MADRID")
                .amount(new BigDecimal("-45.30"))
                .category(category)
                .build();
        entityManager.persist(categorized);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findAll_eagerlyFetchesCategory_withoutExtraLazyLoadQuery() {
        Statistics statistics = statistics();
        statistics.clear();

        Page<Transaction> page = transactionRepository.findAll(PageRequest.of(0, 10));
        long queryCountAfterFetch = statistics.getPrepareStatementCount();

        // Detach everything the repository call just loaded, so that reading the category off
        // of it below can only succeed via data already fetched in the same query (courtesy of
        // @EntityGraph) rather than a fresh per-row lazy-load query against the still-open
        // session/persistence context.
        entityManager.clear();

        assertThat(page.getContent()).hasSize(1);
        Transaction fetched = page.getContent().get(0);

        assertThatCode(() -> fetched.getCategory().getName()).doesNotThrowAnyException();
        assertThat(fetched.getCategory().getId()).isEqualTo(categoryId);
        assertThat(fetched.getCategory().getName()).isEqualTo("Groceries");

        // No additional SQL statement should have been issued by reading the category: it was
        // already fetched in the same query as the transaction.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(queryCountAfterFetch);
    }

    @Test
    void findByCategoryIsNull_returnsUncategorizedTransactions_withCategoryAccessibleAndNull() {
        Transaction uncategorized = Transaction.builder()
                .date(LocalDate.of(2026, 1, 16))
                .description("UNKNOWN MERCHANT XYZ")
                .amount(new BigDecimal("-20.00"))
                .build();
        entityManager.persist(uncategorized);
        entityManager.flush();
        entityManager.clear();

        Page<Transaction> page = transactionRepository.findByCategoryIsNull(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        Transaction fetched = page.getContent().get(0);
        assertThatCode(fetched::getCategory).doesNotThrowAnyException();
        assertThat(fetched.getCategory()).isNull();
    }

    private Statistics statistics() {
        SessionFactory sessionFactory = entityManager.getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        return sessionFactory.getStatistics();
    }
}
