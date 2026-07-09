package com.bankcategorizer.dto;

import java.util.List;

/**
 * Generic paginated response wrapper. Reusable across any endpoint that returns a page of
 * results, not just transactions.
 *
 * @param content       the items on this page
 * @param page          zero-indexed page number
 * @param size          requested page size
 * @param totalElements total number of elements across all pages
 * @param totalPages    total number of pages
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
