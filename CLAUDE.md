# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

A Spring Boot backend that ingests a user's bank statement (CSV/XLSX), auto-categorizes each transaction by matching its description against per-category keywords, and answers spending questions (totals by category over a period, period-over-period comparisons). Single-user, local app — no auth yet.

## Commands

- Build: `mvn clean install`
- Run: `mvn spring-boot:run` (starts on `http://localhost:8080`, `dev` profile active by default)
- Test (full suite): `mvn test`
- Test (single class): `mvn test -Dtest=ClassName`
- Test (single method): `mvn test -Dtest=ClassName#methodName`

Requires a running PostgreSQL instance for the `dev`/`test` profiles (see `src/main/resources/application.yml` — connection defaults to `localhost:5432`, databases `bank_categorizer` / `bank_categorizer_test`, overridable via `DB_USERNAME`/`DB_PASSWORD`). There is no Flyway/Liquibase; schema is managed via `spring.jpa.hibernate.ddl-auto` (`update` in dev, `create-drop` in test, `validate` in prod).

## Architecture

Layered, package-by-layer under `com.bankcategorizer`: `controller` → `service` → `repository`, with `model` (JPA entities) and `dto` (request/response records) as supporting packages, plus `config` for startup wiring and `exception` for the error-handling layer. Controllers stay thin; all business logic lives in services.

### Domain flow across the feature set

1. **Import** (`TransactionImportController` → `TransactionImportService`): parses an uploaded CSV/XLSX into `Transaction` rows (date, description, signed amount — expenses negative, income/deposits positive, matching a real bank statement). Malformed rows are skipped and counted, not fatal. Each parsed row is immediately run through `CategorizationService` before being persisted.
2. **Categorization** (`CategorizationService`): case-insensitive substring match of a transaction's description against each `Category`'s `keywords` set. First category (in creation/id order, via `CategoryRepository.findAllByOrderByIdAsc()`) with a matching keyword wins — there is no scoring, so keyword/category ordering matters when descriptions could match more than one category.
3. **Default seed data** (`config/DefaultCategorySeedData` + `DefaultCategorySeeder`, an `ApplicationRunner`): on a completely empty `categories` table, seeds a fixed set of common categories (Groceries, Dining, Transport, Subscriptions, Shopping, Utilities, Travel, Health, Credits) pre-loaded with well-known merchant keywords, so categorization works out of the box. This only ever fires against an empty table — it never merges into or overwrites existing (including user-created) categories. Edit `DefaultCategorySeedData` to change what gets seeded; no other code needs to change.
4. **Category CRUD** (`CategoryController`/`CategoryService`): create (unique name, optional keywords)/list/delete. Deleting a category uncategorizes (nulls out `category_id` on) its transactions rather than cascading the delete — see `TransactionRepository.uncategorizeByCategoryId`.
5. **Manual (re)categorization** (`TransactionController`/`TransactionService`): lists transactions (optionally filtered to uncategorized-only via `?category=uncategorized`) and lets a transaction's category be corrected via `PATCH`. This never touches `Category.keywords`, so manual corrections don't retroactively change auto-categorization behavior (no implicit "learning").
6. **Spending totals** (`SpendingController`/`SpendingService`): sums the *signed* amounts of matching transactions over a date range, then reports the absolute value as `totalSpent` — this nets out any refund that ended up assigned to a spending category rather than inflating the total. Reused as the single source of truth for "how much was spent in category X over range Y" by the comparison feature below rather than being duplicated.
7. **Period comparison** (`SpendingComparisonService`, under the same `SpendingController`): compares a category's current-calendar-month spend against its previous N months (`lookback`) plus their average. "Current" is resolved from an injected `java.time.Clock` bean (`config/ClockConfig`) rather than `LocalDate.now()`, specifically so tests can fix "today" via `Clock.fixed(...)`. Delegates per-month sums to `SpendingService` rather than reimplementing the sign/abs logic. Only `period=month` is supported today; other granularities are rejected (400), not implemented.

### Error handling

Services throw feature-specific exceptions (`ResourceNotFoundException`, `DuplicateCategoryException`, `InvalidFileFormatException`, `InvalidTransactionFilterException`, `InvalidDateRangeException`, `InvalidSpendingComparisonRequestException`, ...) which `exception/GlobalExceptionHandler` (a single `@RestControllerAdvice`) maps to consistent JSON error bodies and HTTP status codes. There's one exception class per distinct validation concern rather than reusing generic exceptions — follow that pattern when adding new validation.

### Testing

No Flyway/Testcontainers-based integration tests exist yet, despite the Testcontainers dependency being present in `pom.xml` — all current tests are either Mockito-based service unit tests or `@WebMvcTest` MockMvc slice tests for controllers (mocking the service layer). Every service/controller pair added so far has both.
