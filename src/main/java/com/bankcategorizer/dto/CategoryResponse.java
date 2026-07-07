package com.bankcategorizer.dto;

/**
 * Response body representing a spending category.
 */
public record CategoryResponse(Long id, String name, String description) {
}
