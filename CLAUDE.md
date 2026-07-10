# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

A Spring Boot backend that ingests a user's bank statement (CSV/XLSX), auto-categorizes each transaction by matching its description against per-category keywords, and answers spending questions (totals by category over a period, period-over-period comparisons). Single-user, local app — no auth yet.

## Commands

The project ships with the Maven Wrapper (`mvnw` / `mvnw.cmd`), pinned to a fixed
Maven version via `.mvn/wrapper/maven-wrapper.properties` — use it instead of a
globally installed `mvn` so the build isn't at the mercy of whatever Maven happens
to be on the machine. On Windows use `mvnw.cmd` in place of `./mvnw`.

- Build: `./mvnw clean install`
- Run: `./mvnw spring-boot:run` (starts on `http://localhost:8080`, `dev` profile active by default)
- Test (full suite): `./mvnw test`
- Test (single class): `./mvnw test -Dtest=ClassName`
- Test (single method): `./mvnw test -Dtest=ClassName#methodName`

Requires a running PostgreSQL instance for the `dev`/`test` profiles (see `src/main/resources/application.yml` — connection defaults to `localhost:5432`, databases `bank_categorizer` / `bank_categorizer_test`, overridable via `DB_USERNAME`/`DB_PASSWORD`). Schema is managed entirely by Flyway (`src/main/resources/db/migration/`, starting with `V1__baseline_schema.sql`), which runs automatically on startup in every profile; `spring.jpa.hibernate.ddl-auto` is `validate` in `dev`, `test`, and `prod` alike — Hibernate only checks the entities match what Flyway created, it never creates or alters schema itself. `spring.flyway.baseline-on-migrate=true` is set so a pre-existing database created before Flyway was introduced (no `flyway_schema_history` table yet) gets baselined at version 1 instead of Flyway refusing to run. Future schema changes are made by adding new `V{n}__description.sql` migration files, not by editing entities and relying on `ddl-auto=update`.

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

One Testcontainers-based `@SpringBootTest` exists (`ImportCategorizeSpendIntegrationTest`, top-level `com.bankcategorizer` package): it boots the full app with a real embedded HTTP server (`webEnvironment = RANDOM_PORT`) against a real, disposable PostgreSQL container (`@Container` `PostgreSQLContainer<>("postgres:16")`, wired in via `@DynamicPropertySource` so it's hermetic regardless of active profile), driving it end-to-end via `TestRestTemplate` through the import → categorize → spend flow (also implicitly exercising `DefaultCategorySeeder` against a genuinely empty database). Everything else is still either Mockito-based service unit tests or `@WebMvcTest` MockMvc slice tests for controllers (mocking the service layer). Every service/controller pair added so far has both; the Testcontainers flow above is additive, not a replacement for those.
