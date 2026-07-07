package com.bankcategorizer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single row parsed out of an uploaded CSV/XLSX file, before it is mapped to a
 * {@link com.bankcategorizer.model.Transaction} entity. Parsing/validation of raw
 * cell values happens before this DTO is created, so every instance is guaranteed
 * to hold well-formed values.
 */
public record ParsedTransactionRow(LocalDate date, String description, BigDecimal amount) {
}
