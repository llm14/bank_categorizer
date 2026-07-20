# Architecture

## Layered Structure

```
controller (thin, routing only)
    ↓
service (all business logic)
    ↓
repository (data access)
```

Supporting packages:
- `model` — JPA entities (`Transaction`, `Category`)
- `dto` — request/response records
- `config` — startup wiring (`ClockConfig`, `DefaultCategorySeeder`)
- `exception` — error handling (`@RestControllerAdvice` + feature-specific exceptions)

## Domain Flow

### 1. Import
**`TransactionImportController` → `TransactionImportService`**

Parses uploaded CSV/XLSX into `Transaction` rows (date, description, signed amount). Malformed rows are skipped and counted, not fatal. Each parsed row is immediately passed to `CategorizationService` before being persisted.

### 2. Categorization
**`CategorizationService`**

Case-insensitive substring match of transaction description against each `Category`'s keywords. First category (in id/creation order via `CategoryRepository.findAllByOrderByIdAsc()`) with a matching keyword wins. No scoring — keyword/category ordering matters when descriptions could match multiple categories.

### 3. Default Seed Data
**`config/DefaultCategorySeedData` + `DefaultCategorySeeder` (ApplicationRunner)**

On a completely empty `categories` table, seeds a fixed set (Groceries, Dining, Transport, Subscriptions, Shopping, Utilities, Travel, Health, Credits) pre-loaded with merchant keywords. Fires only once against an empty table — never merges or overwrites existing data.

To change what gets seeded, edit `DefaultCategorySeedData.java`. No other code needs to change.

### 4. Category CRUD
**`CategoryController` / `CategoryService`**

Create (unique name, optional keywords), list, delete. Deleting a category uncategorizes (nulls `category_id`) its transactions via `TransactionRepository.uncategorizeByCategoryId()` instead of cascading the delete.

### 5. Manual Recategorization
**`TransactionController` / `TransactionService`**

List transactions (optionally filtered to `?category=uncategorized` via `InvalidTransactionFilterException` validation). Correct a transaction's category via `PATCH`. Never touches `Category.keywords` — corrections are one-off, not retroactive.

### 6. Spending Totals
**`SpendingController` / `SpendingService`**

Sums the *signed* amounts of matching transactions over a date range, then reports the absolute value as `totalSpent`. Single source of truth for "how much was spent in category X over range Y" — reused by the comparison feature below.

### 7. Period Comparison
**`SpendingComparisonService` (under `SpendingController`)**

Compares a category's current-calendar-month spend against previous N months (`lookback`) plus their average. "Current" is resolved from injected `Clock` bean for testability. Delegates per-month sums to `SpendingService`. Only `period=month` supported; other granularities rejected (400).

## Error Handling

Services throw feature-specific exceptions:
- `ResourceNotFoundException`
- `DuplicateCategoryException`
- `InvalidFileFormatException`
- `InvalidTransactionFilterException`
- `InvalidDateRangeException`
- `InvalidSpendingComparisonRequestException`

`@RestControllerAdvice` in `exception/GlobalExceptionHandler` maps each to consistent JSON error body and HTTP status code. One exception class per distinct validation concern — follow this pattern when adding new validation.

## Testing Strategy

- **MockMvc `@WebMvcTest`**: Controllers with mocked service layer
- **Mockito unit tests**: Services in isolation
- **Testcontainers `@SpringBootTest`**: Full integration — real embedded HTTP server (`RANDOM_PORT`), real PostgreSQL container, end-to-end import → categorize → spend flow (implicitly exercises `DefaultCategorySeeder` against genuinely empty database)

The Testcontainers test is additive, not a replacement for controller/service unit tests.
