package com.bankcategorizer.dto;

import java.util.List;

/**
 * Response body representing a spending category.
 */
public record CategoryResponse(Long id, String name, String description, List<String> keywords) {
}
