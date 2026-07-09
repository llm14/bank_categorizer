package com.bankcategorizer;

import com.bankcategorizer.dto.CategoryResponse;
import com.bankcategorizer.dto.ImportResultResponse;
import com.bankcategorizer.dto.PageResponse;
import com.bankcategorizer.dto.SpendingResponse;
import com.bankcategorizer.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test of the import -> categorize -> spend flow against a real, disposable
 * PostgreSQL instance (via Testcontainers) and the real embedded HTTP stack (no MockMvc, no
 * mocked repositories). This also implicitly exercises {@code DefaultCategorySeeder} against a
 * genuinely empty database on application startup.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImportCategorizeSpendIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @LocalServerPort
    private int port;

    @Test
    void importCategorizeSpend_endToEnd_flowsThroughRealStack() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        String baseUrl = "http://localhost:" + port;

        // (a) Default categories are seeded against the real, fresh database on startup.
        ResponseEntity<List<CategoryResponse>> categoriesResponse = restTemplate.exchange(
                baseUrl + "/api/v1/categories", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<CategoryResponse>>() {
                });

        assertThat(categoriesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<CategoryResponse> categories = categoriesResponse.getBody();
        assertThat(categories).isNotNull();

        Optional<CategoryResponse> groceries = categories.stream()
                .filter(category -> "Groceries".equals(category.name()))
                .findFirst();
        assertThat(groceries).isPresent();
        assertThat(groceries.get().keywords()).contains("MERCADONA");
        Long groceriesCategoryId = groceries.get().id();

        // (b) Upload a small CSV: one row matches a seeded Groceries keyword, the other doesn't
        // match anything and should stay uncategorized.
        String csvContent = """
                Date,Description,Amount
                2026-01-15,MERCADONA MADRID,-45.30
                2026-01-16,UNKNOWN MERCHANT XYZ,-20.00
                """;

        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "transactions.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> importRequest = new HttpEntity<>(body, headers);

        ResponseEntity<ImportResultResponse> importResponse = restTemplate.postForEntity(
                baseUrl + "/api/v1/transactions/import", importRequest, ImportResultResponse.class);

        // (c) Import response reports the correct counts.
        assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ImportResultResponse importResult = importResponse.getBody();
        assertThat(importResult).isNotNull();
        assertThat(importResult.importedCount()).isEqualTo(2);
        assertThat(importResult.categorizedCount()).isEqualTo(1);
        assertThat(importResult.uncategorizedCount()).isEqualTo(1);

        // (d) The persisted transactions reflect real JPA/Hibernate mapping against Postgres.
        ResponseEntity<PageResponse<TransactionResponse>> transactionsResponse = restTemplate.exchange(
                baseUrl + "/api/v1/transactions", HttpMethod.GET, null,
                new ParameterizedTypeReference<PageResponse<TransactionResponse>>() {
                });

        assertThat(transactionsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<TransactionResponse> transactionsPage = transactionsResponse.getBody();
        assertThat(transactionsPage).isNotNull();
        assertThat(transactionsPage.totalElements()).isEqualTo(2);
        List<TransactionResponse> transactions = transactionsPage.content();
        assertThat(transactions).hasSize(2);

        TransactionResponse groceriesTransaction = transactions.stream()
                .filter(transaction -> "MERCADONA MADRID".equals(transaction.description()))
                .findFirst()
                .orElseThrow();
        assertThat(groceriesTransaction.categoryId()).isEqualTo(groceriesCategoryId);
        assertThat(groceriesTransaction.categoryName()).isEqualTo("Groceries");

        TransactionResponse uncategorizedTransaction = transactions.stream()
                .filter(transaction -> "UNKNOWN MERCHANT XYZ".equals(transaction.description()))
                .findFirst()
                .orElseThrow();
        assertThat(uncategorizedTransaction.categoryId()).isNull();
        assertThat(uncategorizedTransaction.categoryName()).isNull();

        // (e) Spending total for Groceries is computed by the real aggregation query, not a mock.
        String spendingUrl = baseUrl + "/api/v1/spending?category=" + groceriesCategoryId
                + "&from=2026-01-01&to=2026-01-31";
        ResponseEntity<SpendingResponse> spendingResponse = restTemplate.getForEntity(
                spendingUrl, SpendingResponse.class);

        assertThat(spendingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        SpendingResponse spending = spendingResponse.getBody();
        assertThat(spending).isNotNull();
        assertThat(spending.categoryId()).isEqualTo(groceriesCategoryId);
        assertThat(spending.totalSpent()).isEqualByComparingTo(new BigDecimal("45.30"));

        // (f) The category breakdown is computed by the real GROUP BY projection query (JPQL
        // "SELECT NEW ...record..." constructor expression) against Postgres, not a mock, and
        // excludes the uncategorized transaction.
        String breakdownUrl = baseUrl + "/api/v1/spending?from=2026-01-01&to=2026-01-31";
        ResponseEntity<List<SpendingResponse>> breakdownResponse = restTemplate.exchange(
                breakdownUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<SpendingResponse>>() {
                });

        assertThat(breakdownResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<SpendingResponse> breakdown = breakdownResponse.getBody();
        assertThat(breakdown).isNotNull();
        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.get(0).categoryId()).isEqualTo(groceriesCategoryId);
        assertThat(breakdown.get(0).totalSpent()).isEqualByComparingTo(new BigDecimal("45.30"));
    }
}
