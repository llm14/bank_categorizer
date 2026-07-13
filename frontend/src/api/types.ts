/**
 * TypeScript mirrors of the backend's DTOs (src/main/java/com/bankcategorizer/dto).
 * Keep field names/types in sync with the Java records - they are the source of truth.
 *
 * Java `LocalDate` -> ISO date string ("YYYY-MM-DD").
 * Java `OffsetDateTime` -> ISO datetime string.
 * Java `BigDecimal` -> number (transported as a JSON number).
 */

/** Mirrors com.bankcategorizer.dto.ErrorResponse. */
export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}

/** Mirrors com.bankcategorizer.dto.CategoryResponse. */
export interface CategoryResponse {
  id: number;
  name: string;
  description: string | null;
  keywords: string[];
}

/** Mirrors com.bankcategorizer.dto.CategoryCreateRequest. */
export interface CategoryCreateRequest {
  name: string;
  description?: string | null;
  keywords?: string[];
}

/** Mirrors com.bankcategorizer.dto.ImportResultResponse. */
export interface ImportResultResponse {
  fileName: string;
  totalRows: number;
  importedCount: number;
  skippedCount: number;
  categorizedCount: number;
  uncategorizedCount: number;
}

/**
 * Mirrors com.bankcategorizer.dto.TransactionResponse. categoryId/categoryName are both
 * null when the transaction is uncategorized.
 */
export interface TransactionResponse {
  id: number;
  date: string;
  description: string;
  amount: number;
  categoryId: number | null;
  categoryName: string | null;
}

/** Mirrors com.bankcategorizer.dto.TransactionUpdateRequest. */
export interface TransactionUpdateRequest {
  categoryId: number;
}

/**
 * Mirrors com.bankcategorizer.dto.TransactionCreateRequest. Follows the same signed-amount
 * convention as everywhere else (expenses negative, income positive) - no sign flipping here.
 */
export interface TransactionCreateRequest {
  date: string;
  description: string;
  amount: number;
}

/** Mirrors com.bankcategorizer.dto.SpendingResponse. */
export interface SpendingResponse {
  categoryId: number;
  categoryName: string;
  from: string;
  to: string;
  totalSpent: number;
}

/** Mirrors com.bankcategorizer.dto.SpendingBreakdownResponse. */
export interface SpendingBreakdownResponse {
  breakdown: SpendingResponse[];
  totalSpent: number;
}

/** Mirrors com.bankcategorizer.dto.PeriodSpending. */
export interface PeriodSpending {
  label: string;
  from: string;
  to: string;
  totalSpent: number;
}

/**
 * Mirrors com.bankcategorizer.dto.SpendingComparisonResponse. categoryId/categoryName are
 * both null when comparing across all categories combined (category omitted from the
 * request) - see SpendingComparisonService's null-categoryId branch.
 */
export interface SpendingComparisonResponse {
  categoryId: number | null;
  categoryName: string | null;
  period: string;
  lookback: number;
  current: PeriodSpending;
  previousPeriods: PeriodSpending[];
  previousAverage: number;
}

/** Mirrors com.bankcategorizer.dto.PageResponse<T>. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/**
 * Shape returned by Spring Boot Actuator's `/actuator/health` endpoint. Only `status` is
 * guaranteed; `components` is present when the endpoint is configured to show details.
 */
export interface HealthResponse {
  status: string;
  components?: Record<string, unknown>;
}

/** Mirrors com.bankcategorizer.dto.LoginRequest. */
export interface LoginRequest {
  username: string;
  password: string;
}

/** Mirrors com.bankcategorizer.dto.LoginResponse. */
export interface LoginResponse {
  token: string;
}

/** The only supported filter value for `GET /api/v1/transactions?category=...`. */
export const UNCATEGORIZED_FILTER = "uncategorized";

export type TransactionCategoryFilter = typeof UNCATEGORIZED_FILTER;
