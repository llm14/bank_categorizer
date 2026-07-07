package com.bankcategorizer.dto;

/**
 * Summary of a transaction file import: how many data rows were found, how many
 * were successfully persisted as (uncategorized) transactions, and how many were
 * skipped because they could not be parsed.
 */
public record ImportResultResponse(String fileName, int totalRows, int importedCount, int skippedCount) {
}
