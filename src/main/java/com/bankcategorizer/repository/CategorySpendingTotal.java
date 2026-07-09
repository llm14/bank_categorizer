package com.bankcategorizer.repository;

import java.math.BigDecimal;

/**
 * Query-shape projection for a category's net signed amount total over a date range,
 * produced by a {@code GROUP BY} aggregate query. Not a REST-facing type; see
 * {@code com.bankcategorizer.dto.SpendingResponse} for that.
 */
public record CategorySpendingTotal(
        Long categoryId,
        String categoryName,
        BigDecimal totalAmount
) {
}
