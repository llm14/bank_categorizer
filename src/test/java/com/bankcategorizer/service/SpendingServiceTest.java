package com.bankcategorizer.service;

import com.bankcategorizer.dto.SpendingResponse;
import com.bankcategorizer.exception.InvalidDateRangeException;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.model.Category;
import com.bankcategorizer.model.Transaction;
import com.bankcategorizer.repository.CategoryRepository;
import com.bankcategorizer.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpendingServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private SpendingService spendingService;

    private final LocalDate from = LocalDate.of(2024, 1, 1);
    private final LocalDate to = LocalDate.of(2024, 1, 31);

    @BeforeEach
    void setUp() {
        spendingService = new SpendingService(transactionRepository, categoryRepository);
    }

    @Test
    void getSpendingForCategory_mixedPositiveAndNegativeAmounts_returnsAbsoluteOfNetSum() {
        Category groceries = Category.builder().id(1L).name("Groceries").build();
        Transaction expense = Transaction.builder()
                .id(1L).date(from).description("Walmart").amount(new BigDecimal("-54.32")).category(groceries).build();
        Transaction refund = Transaction.builder()
                .id(2L).date(from).description("Walmart refund").amount(new BigDecimal("10.00")).category(groceries).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(groceries));
        when(transactionRepository.findByCategoryIdAndDateBetween(1L, from, to)).thenReturn(List.of(expense, refund));

        SpendingResponse response = spendingService.getSpendingForCategory(1L, from, to);

        assertThat(response.categoryId()).isEqualTo(1L);
        assertThat(response.categoryName()).isEqualTo("Groceries");
        assertThat(response.totalSpent()).isEqualByComparingTo("44.32");
    }

    @Test
    void getSpendingForCategory_missingCategory_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spendingService.getSpendingForCategory(99L, from, to))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).findByCategoryIdAndDateBetween(any(), any(), any());
    }

    @Test
    void getSpendingForCategory_noMatchingTransactions_returnsZeroTotal() {
        Category groceries = Category.builder().id(1L).name("Groceries").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(groceries));
        when(transactionRepository.findByCategoryIdAndDateBetween(1L, from, to)).thenReturn(List.of());

        SpendingResponse response = spendingService.getSpendingForCategory(1L, from, to);

        assertThat(response.totalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getSpendingForCategory_invalidRange_throwsInvalidDateRangeException() {
        assertThatThrownBy(() -> spendingService.getSpendingForCategory(1L, to, from))
                .isInstanceOf(InvalidDateRangeException.class);

        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void getSpendingForCategory_missingDates_throwsInvalidDateRangeException() {
        assertThatThrownBy(() -> spendingService.getSpendingForCategory(1L, null, to))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void getSpendingBreakdown_multipleCategories_returnsOneEntryPerCategory() {
        Category groceries = Category.builder().id(1L).name("Groceries").build();
        Category transport = Category.builder().id(2L).name("Transport").build();
        Transaction groceriesTx = Transaction.builder()
                .id(1L).date(from).description("Supermarket").amount(new BigDecimal("-20.00")).category(groceries).build();
        Transaction transportTx = Transaction.builder()
                .id(2L).date(from).description("Bus ticket").amount(new BigDecimal("-5.00")).category(transport).build();
        when(transactionRepository.findByDateBetween(from, to)).thenReturn(List.of(groceriesTx, transportTx));

        List<SpendingResponse> breakdown = spendingService.getSpendingBreakdown(from, to);

        assertThat(breakdown).hasSize(2);
        assertThat(breakdown).extracting(SpendingResponse::categoryName).containsExactlyInAnyOrder("Groceries", "Transport");
        assertThat(breakdown).extracting(SpendingResponse::totalSpent)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(new BigDecimal("20.00"), new BigDecimal("5.00"));
    }

    @Test
    void getSpendingBreakdown_excludesUncategorizedTransactions() {
        Category groceries = Category.builder().id(1L).name("Groceries").build();
        Transaction groceriesTx = Transaction.builder()
                .id(1L).date(from).description("Supermarket").amount(new BigDecimal("-20.00")).category(groceries).build();
        Transaction uncategorizedTx = Transaction.builder()
                .id(2L).date(from).description("Unknown merchant").amount(new BigDecimal("-15.00")).build();
        when(transactionRepository.findByDateBetween(from, to)).thenReturn(List.of(groceriesTx, uncategorizedTx));

        List<SpendingResponse> breakdown = spendingService.getSpendingBreakdown(from, to);

        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.get(0).categoryName()).isEqualTo("Groceries");
    }

    @Test
    void getSpendingBreakdown_noTransactionsInRange_returnsEmptyList() {
        when(transactionRepository.findByDateBetween(from, to)).thenReturn(List.of());

        List<SpendingResponse> breakdown = spendingService.getSpendingBreakdown(from, to);

        assertThat(breakdown).isEmpty();
    }

    @Test
    void getSpendingBreakdown_invalidRange_throwsInvalidDateRangeException() {
        assertThatThrownBy(() -> spendingService.getSpendingBreakdown(to, from))
                .isInstanceOf(InvalidDateRangeException.class);

        verify(transactionRepository, never()).findByDateBetween(any(), any());
    }
}
