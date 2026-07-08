package com.bankcategorizer.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for manually assigning or correcting a transaction's category.
 */
public record TransactionUpdateRequest(

        @NotNull(message = "categoryId is required")
        Long categoryId
) {
}
