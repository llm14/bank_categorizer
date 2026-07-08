package com.bankcategorizer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response body representing a bank transaction. {@code categoryId} and {@code categoryName}
 * are both {@code null} when the transaction is uncategorized.
 */
public record TransactionResponse(
        Long id,
        LocalDate date,
        String description,
        BigDecimal amount,
        Long categoryId,
        String categoryName
) {
}
