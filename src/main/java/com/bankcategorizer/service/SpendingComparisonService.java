package com.bankcategorizer.service;

import com.bankcategorizer.dto.PeriodSpending;
import com.bankcategorizer.dto.SpendingComparisonResponse;
import com.bankcategorizer.dto.SpendingResponse;
import com.bankcategorizer.exception.InvalidSpendingComparisonRequestException;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.model.Category;
import com.bankcategorizer.repository.CategoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for comparing a category's spending in the current period against its
 * previous periods (e.g. "groceries this month vs. the average of the last 3 months").
 *
 * <p>Only "month" (calendar month) is currently supported as a period granularity; the
 * "current period" is the calendar month containing {@link Clock#instant()}, resolved via an
 * injected {@link Clock} rather than {@code LocalDate.now()} so it stays testable.
 */
@Service
public class SpendingComparisonService {

    private static final String MONTH_PERIOD = "month";
    private static final int AVERAGE_SCALE = 2;

    private final Clock clock;
    private final CategoryRepository categoryRepository;
    private final SpendingService spendingService;

    public SpendingComparisonService(Clock clock, CategoryRepository categoryRepository, SpendingService spendingService) {
        this.clock = clock;
        this.categoryRepository = categoryRepository;
        this.spendingService = spendingService;
    }

    @Transactional(readOnly = true)
    public SpendingComparisonResponse compare(Long categoryId, String period, Integer lookback) {
        validatePeriod(period);
        validateLookback(lookback);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category %d not found".formatted(categoryId)));

        YearMonth currentMonth = YearMonth.now(clock);
        PeriodSpending current = spendingForMonth(categoryId, currentMonth);

        List<PeriodSpending> previousPeriods = new ArrayList<>();
        for (int i = 1; i <= lookback; i++) {
            previousPeriods.add(spendingForMonth(categoryId, currentMonth.minusMonths(i)));
        }

        BigDecimal previousAverage = averageOf(previousPeriods);

        return new SpendingComparisonResponse(
                category.getId(),
                category.getName(),
                MONTH_PERIOD,
                lookback,
                current,
                previousPeriods,
                previousAverage);
    }

    private PeriodSpending spendingForMonth(Long categoryId, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        SpendingResponse spending = spendingService.getSpendingForCategory(categoryId, from, to);
        return new PeriodSpending(month.toString(), from, to, spending.totalSpent());
    }

    private BigDecimal averageOf(List<PeriodSpending> periods) {
        if (periods.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = periods.stream()
                .map(PeriodSpending::totalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(periods.size()), AVERAGE_SCALE, RoundingMode.HALF_UP);
    }

    private void validatePeriod(String period) {
        if (period == null || !MONTH_PERIOD.equalsIgnoreCase(period)) {
            throw new InvalidSpendingComparisonRequestException(
                    "Unsupported period '%s': only 'month' is currently supported".formatted(period));
        }
    }

    private void validateLookback(Integer lookback) {
        if (lookback == null || lookback < 1) {
            throw new InvalidSpendingComparisonRequestException(
                    "'lookback' must be a positive integer");
        }
    }
}
