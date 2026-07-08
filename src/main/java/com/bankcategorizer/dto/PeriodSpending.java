package com.bankcategorizer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The total amount spent during a single period (e.g. one calendar month) within a
 * spending comparison. {@code label} is the ISO representation of the period, e.g.
 * {@code "2026-07"} for a calendar month.
 */
public record PeriodSpending(
        String label,
        LocalDate from,
        LocalDate to,
        BigDecimal totalSpent
) {
}
