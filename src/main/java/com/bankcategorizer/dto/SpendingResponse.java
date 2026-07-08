package com.bankcategorizer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response body representing the total amount spent in a category over a date range.
 * {@code totalSpent} is always non-negative: it is the absolute value of the net (signed)
 * sum of matching transaction amounts, so refunds naturally offset expenses.
 */
public record SpendingResponse(
        Long categoryId,
        String categoryName,
        LocalDate from,
        LocalDate to,
        BigDecimal totalSpent
) {
}
