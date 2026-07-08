package com.bankcategorizer.dto;

/**
 * Summary of a transaction file import: how many data rows were found, how many were
 * successfully persisted as transactions, and how many were skipped because they could not
 * be parsed. Of the persisted transactions, {@code categorizedCount} were auto-assigned a
 * category based on a matching keyword and {@code uncategorizedCount} were left
 * uncategorized (no matching category); {@code categorizedCount + uncategorizedCount}
 * always equals {@code importedCount}.
 */
public record ImportResultResponse(
        String fileName,
        int totalRows,
        int importedCount,
        int skippedCount,
        int categorizedCount,
        int uncategorizedCount
) {
}
