package com.bankcategorizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new spending category.
 */
public record CategoryCreateRequest(

        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @Size(max = 255, message = "description must be at most 255 characters")
        String description
) {
}
