package com.bankcategorizer.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response body comparing a category's spending in the current period against each of the
 * {@code lookback} previous periods (most-recent-first), along with the average of those
 * previous periods' totals.
 */
public record SpendingComparisonResponse(
        Long categoryId,
        String categoryName,
        String period,
        int lookback,
        PeriodSpending current,
        List<PeriodSpending> previousPeriods,
        BigDecimal previousAverage
) {
}
