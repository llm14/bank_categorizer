package com.bankcategorizer.service;

import com.bankcategorizer.dto.SpendingBreakdownResponse;
import com.bankcategorizer.dto.SpendingComparisonResponse;
import com.bankcategorizer.dto.SpendingResponse;
import com.bankcategorizer.exception.InvalidSpendingComparisonRequestException;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.model.Category;
import com.bankcategorizer.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpendingComparisonServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SpendingService spendingService;

    private SpendingComparisonService spendingComparisonService;

    // "Today" fixed at 2026-07-08, so the current period is 2026-07.
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-07-08T00:00:00Z"), ZoneOffset.UTC);

    private final Category groceries = Category.builder().id(1L).name("Groceries").build();

    @BeforeEach
    void setUp() {
        spendingComparisonService = new SpendingComparisonService(fixedClock, categoryRepository, spendingService);
    }

    @Test
    void compare_happyPath_returnsCurrentAndPreviousPeriodsMostRecentFirst() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(groceries));
        mockSpending(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "100.00");
        mockSpending(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "50.00");
        mockSpending(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "60.00");
        mockSpending(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), "40.00");

        SpendingComparisonResponse response = spendingComparisonService.compare(1L, "month", 3);

        assertThat(response.categoryId()).isEqualTo(1L);
        assertThat(response.categoryName()).isEqualTo("Groceries");
        assertThat(response.period()).isEqualTo("month");
        assertThat(response.lookback()).isEqualTo(3);

        assertThat(response.current().label()).isEqualTo("2026-07");
        assertThat(response.current().from()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.current().to()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(response.current().totalSpent()).isEqualByComparingTo("100.00");

        assertThat(response.previousPeriods()).hasSize(3);
        assertThat(response.previousPeriods()).extracting("label")
                .containsExactly("2026-06", "2026-05", "2026-04");
        assertThat(response.previousPeriods().get(0).totalSpent()).isEqualByComparingTo("50.00");
        assertThat(response.previousPeriods().get(1).totalSpent()).isEqualByComparingTo("60.00");
        assertThat(response.previousPeriods().get(2).totalSpent()).isEqualByComparingTo("40.00");
    }

    @Test
    void compare_previousAverage_isComputedFromPreviousPeriodsOnly() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(groceries));
        mockSpending(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "999.00");
        mockSpending(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "50.00");
        mockSpending(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "61.00");

        SpendingComparisonResponse response = spendingComparisonService.compare(1L, "month", 2);

        // average(50.00, 61.00) = 55.50
        assertThat(response.previousAverage()).isEqualByComparingTo("55.50");
    }

    @Test
    void compare_averageRoundsHalfUpToTwoDecimals() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(groceries));
        mockSpending(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "0.00");
        mockSpending(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "10.00");
        mockSpending(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "10.00");
        mockSpending(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), "10.01");

        SpendingComparisonResponse response = spendingComparisonService.compare(1L, "month", 3);

        // average(10.00, 10.00, 10.01) = 30.01 / 3 = 10.003... -> rounds to 10.00
        assertThat(response.previousAverage()).isEqualByComparingTo("10.00");
    }

    @Test
    void compare_periodCaseInsensitive_isAccepted() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(groceries));
        mockSpending(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "5.00");
        mockSpending(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "5.00");

        SpendingComparisonResponse response = spendingComparisonService.compare(1L, "MONTH", 1);

        assertThat(response.period()).isEqualTo("month");
    }

    @Test
    void compare_nullCategoryId_comparesAllCategoriesAndSkipsCategoryLookup() {
        mockBreakdownTotal(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "300.00");
        mockBreakdownTotal(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "100.00");
        mockBreakdownTotal(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "50.00");

        SpendingComparisonResponse response = spendingComparisonService.compare(null, "month", 2);

        assertThat(response.categoryId()).isNull();
        assertThat(response.categoryName()).isNull();
        assertThat(response.period()).isEqualTo("month");
        assertThat(response.lookback()).isEqualTo(2);

        assertThat(response.current().label()).isEqualTo("2026-07");
        assertThat(response.current().totalSpent()).isEqualByComparingTo("300.00");

        assertThat(response.previousPeriods()).hasSize(2);
        assertThat(response.previousPeriods().get(0).totalSpent()).isEqualByComparingTo("100.00");
        assertThat(response.previousPeriods().get(1).totalSpent()).isEqualByComparingTo("50.00");

        // average(100.00, 50.00) = 75.00
        assertThat(response.previousAverage()).isEqualByComparingTo("75.00");

        verify(categoryRepository, never()).findById(any());
        verify(spendingService, never()).getSpendingForCategory(any(), any(), any());
    }

    @Test
    void compare_categoryNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spendingComparisonService.compare(99L, "month", 3))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(spendingService, never()).getSpendingForCategory(any(), any(), any());
    }

    @Test
    void compare_unsupportedPeriod_throwsInvalidSpendingComparisonRequestException() {
        assertThatThrownBy(() -> spendingComparisonService.compare(1L, "week", 3))
                .isInstanceOf(InvalidSpendingComparisonRequestException.class);

        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void compare_missingPeriod_throwsInvalidSpendingComparisonRequestException() {
        assertThatThrownBy(() -> spendingComparisonService.compare(1L, null, 3))
                .isInstanceOf(InvalidSpendingComparisonRequestException.class);
    }

    @Test
    void compare_missingLookback_throwsInvalidSpendingComparisonRequestException() {
        assertThatThrownBy(() -> spendingComparisonService.compare(1L, "month", null))
                .isInstanceOf(InvalidSpendingComparisonRequestException.class);
    }

    @Test
    void compare_zeroLookback_throwsInvalidSpendingComparisonRequestException() {
        assertThatThrownBy(() -> spendingComparisonService.compare(1L, "month", 0))
                .isInstanceOf(InvalidSpendingComparisonRequestException.class);
    }

    @Test
    void compare_negativeLookback_throwsInvalidSpendingComparisonRequestException() {
        assertThatThrownBy(() -> spendingComparisonService.compare(1L, "month", -1))
                .isInstanceOf(InvalidSpendingComparisonRequestException.class);
    }

    @Test
    void compare_lookbackExceedsMaximum_throwsInvalidSpendingComparisonRequestException() {
        assertThatThrownBy(() -> spendingComparisonService.compare(1L, "month", 240))
                .isInstanceOf(InvalidSpendingComparisonRequestException.class);

        verify(categoryRepository, never()).findById(any());
    }

    private void mockSpending(LocalDate from, LocalDate to, String totalSpent) {
        when(spendingService.getSpendingForCategory(eq(1L), eq(from), eq(to)))
                .thenReturn(new SpendingResponse(1L, "Groceries", from, to, new BigDecimal(totalSpent)));
    }

    private void mockBreakdownTotal(LocalDate from, LocalDate to, String totalSpent) {
        when(spendingService.getSpendingBreakdown(eq(from), eq(to)))
                .thenReturn(new SpendingBreakdownResponse(List.of(), new BigDecimal(totalSpent)));
    }
}
