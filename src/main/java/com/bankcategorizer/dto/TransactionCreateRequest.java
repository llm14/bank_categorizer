package com.bankcategorizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for manually adding a single transaction. Follows the same signed-amount
 * convention as imported transactions (expenses negative, income/deposits positive) — no
 * special-case sign handling for manually-added rows.
 */
public record TransactionCreateRequest(

        @NotNull(message = "date is required")
        LocalDate date,

        @NotBlank(message = "description is required")
        String description,

        @NotNull(message = "amount is required")
        BigDecimal amount
) {
}
