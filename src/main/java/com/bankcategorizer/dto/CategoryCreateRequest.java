package com.bankcategorizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for creating a new spending category.
 *
 * <p>{@code keywords} is optional: when provided, a transaction is auto-assigned to this
 * category on import if any keyword appears as a case-insensitive substring of its
 * description. Blank entries are ignored.
 */
public record CategoryCreateRequest(

        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @Size(max = 255, message = "description must be at most 255 characters")
        String description,

        List<String> keywords
) {
}
