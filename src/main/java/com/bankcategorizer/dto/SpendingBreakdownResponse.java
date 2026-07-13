package com.bankcategorizer.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response body for the all-categories spending breakdown over a date range: the
 * per-category {@link SpendingResponse} entries, plus the total spent across every
 * category included in that range.
 *
 * <p>{@code totalSpent} is the sum of each entry's already-absolute {@code totalSpent} —
 * i.e. exactly what a caller adding up the displayed breakdown themselves would get, not a
 * re-derived signed-sum-then-abs across categories (so a refund in one category doesn't net
 * against another category's expenses).
 */
public record SpendingBreakdownResponse(
        List<SpendingResponse> breakdown,
        BigDecimal totalSpent
) {
}
