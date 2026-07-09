package com.bankcategorizer.service;

import com.bankcategorizer.dto.SpendingResponse;
import com.bankcategorizer.exception.InvalidDateRangeException;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.model.Category;
import com.bankcategorizer.repository.CategoryRepository;
import com.bankcategorizer.repository.CategorySpendingTotal;
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
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(groceries));
        when(transactionRepository.sumAmountByCategoryAndDateBetween(1L, from, to)).thenReturn(new BigDecimal("-44.32"));

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

        verify(transactionRepository, never()).sumAmountByCategoryAndDateBetween(any(), any(), any());
    }

    @Test
    void getSpendingForCategory_noMatchingTransactions_returnsZeroTotal() {
        Category groceries = Category.builder().id(1L).name("Groceries").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(groceries));
        when(transactionRepository.sumAmountByCategoryAndDateBetween(1L, from, to)).thenReturn(BigDecimal.ZERO);

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
        when(transactionRepository.sumAmountByCategoryGroupedForDateBetween(from, to)).thenReturn(List.of(
                new CategorySpendingTotal(1L, "Groceries", new BigDecimal("-20.00")),
                new CategorySpendingTotal(2L, "Transport", new BigDecimal("-5.00"))
        ));

        List<SpendingResponse> breakdown = spendingService.getSpendingBreakdown(from, to);

        assertThat(breakdown).hasSize(2);
        assertThat(breakdown).extracting(SpendingResponse::categoryName).containsExactlyInAnyOrder("Groceries", "Transport");
        assertThat(breakdown).extracting(SpendingResponse::totalSpent)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(new BigDecimal("20.00"), new BigDecimal("5.00"));
    }

    @Test
    void getSpendingBreakdown_excludesUncategorizedTransactions() {
        // The repository query filters out uncategorized transactions in SQL (t.category IS NOT
        // NULL), so a category-less transaction never appears in the projected result set.
        when(transactionRepository.sumAmountByCategoryGroupedForDateBetween(from, to)).thenReturn(List.of(
                new CategorySpendingTotal(1L, "Groceries", new BigDecimal("-20.00"))
        ));

        List<SpendingResponse> breakdown = spendingService.getSpendingBreakdown(from, to);

        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.get(0).categoryName()).isEqualTo("Groceries");
    }

    @Test
    void getSpendingBreakdown_noTransactionsInRange_returnsEmptyList() {
        when(transactionRepository.sumAmountByCategoryGroupedForDateBetween(from, to)).thenReturn(List.of());

        List<SpendingResponse> breakdown = spendingService.getSpendingBreakdown(from, to);

        assertThat(breakdown).isEmpty();
    }

    @Test
    void getSpendingBreakdown_invalidRange_throwsInvalidDateRangeException() {
        assertThatThrownBy(() -> spendingService.getSpendingBreakdown(to, from))
                .isInstanceOf(InvalidDateRangeException.class);

        verify(transactionRepository, never()).sumAmountByCategoryGroupedForDateBetween(any(), any());
    }
}
