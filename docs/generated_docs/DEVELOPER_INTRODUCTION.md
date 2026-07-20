# Developer Introduction

This document explains how bank_categorizer is organized, why it's built this way, and how the pieces work together. If you're joining the project or reviewing the codebase, start here to understand the overall shape, architecture decisions, and design philosophy before diving into specific features or tasks.

## Architecture: Layered for Testability and Clarity

Bank_categorizer follows a layered architecture where responsibility is strictly separated: **controllers route HTTP requests**, **services hold all business logic**, and **repositories handle data access**. This separation exists for a reason: it makes the system testable. You can test service logic in isolation (without HTTP), mock services when testing controllers, and run full integration tests against a real database. Controllers stay thin—they never contain conditionals, loops, or business rules. If you're tempted to add logic to a controller, it belongs in a service instead. Supporting packages (`model`, `dto`, `config`, `exception`) handle JPA entities, request/response contracts, startup wiring, and error mapping respectively.

## Database: Flyway Is Source of Truth

The database schema is managed by Flyway migration files (`V{n}__description.sql` in `src/main/resources/db/migration/`), not by Hibernate entity definitions. Hibernate is set to `ddl-auto=validate`, meaning it checks that the database matches the entities but never creates or alters tables. This constraint is non-negotiable: when you need a schema change, write a Flyway migration file, apply it to your local database, and commit both the migration and the updated entity. Never rely on Hibernate to generate schema or edit migration files after they've been committed. This approach prevents the silent schema drift that happens when developers rely on tools to auto-generate DDL.

## Categorization: Keyword Matching, First Match Wins

When a transaction is imported, the system assigns it a category by performing a case-insensitive substring search of the transaction's description against each category's keywords. The first category (ordered by `id`/creation date) with a matching keyword wins—there is no scoring mechanism. This simplicity is deliberate. If a description could match multiple categories, order matters, so merchants are grouped into categories carefully. Manual corrections (assigning a different category to a transaction after the fact) never change the categorization behavior. Corrections are one-off—they don't "teach" the system or retroactively recategorize other transactions. The auto-categorization rules stay static until you explicitly edit the category keywords.

## Transaction Flow: From Import to Query

The journey of a transaction is straightforward: *import → categorize → persist → query*. When you upload a CSV or XLSX file, `TransactionImportService` parses each row into a `Transaction` object (date, description, signed amount). Malformed rows are skipped and counted but never fatal to the import. Each parsed row is immediately passed to `CategorizationService` before being saved to the database. Once persisted, transactions can be listed, filtered, manually recategorized, or aggregated into spending totals. Spending totals always use *signed* amounts (so refunds appear as negative), then report the absolute value. If a category is deleted, its transactions are uncategorized (their `category_id` is set to null) rather than deleted. This flow is exercised end-to-end in the Testcontainers integration test.

## Testing: Three Layers, Each With Purpose

The test suite has three levels of coverage. **`@WebMvcTest` tests** use MockMvc to verify controller routing and response contracts while mocking the service layer—they're fast and focused on HTTP semantics. **Mockito unit tests** exercise service and repository logic in isolation, with no database or HTTP involved. **Testcontainers integration tests** spin up a real PostgreSQL container and real embedded HTTP server on a random port, then drive the full import → categorize → spend flow end-to-end. This last layer proves that the pieces actually fit together. The Testcontainers test is additive, not a replacement for unit tests; each level catches different problems. Additionally, `Clock` is always injected (never `LocalDate.now()`), so tests can freeze time via `Clock.fixed(...)` for deterministic results.

## Design Decisions: Why Constraints Exist

Several design choices may seem strict at first. **One exception class per validation concern**: don't reuse generic exceptions; create `DuplicateCategoryException` for one problem and `InvalidDateRangeException` for another. This forces each error to be explicit and makes `@RestControllerAdvice` exception mapping unambiguous. **Single user scope**: the app assumes one user and no authentication—transactions, categories, and queries are global. **Period comparison limited to months**: the API only supports `period=month` for spending comparisons; other granularities are rejected with HTTP 400, not implemented halfway. **Signed amounts**: transactions store signed values (expenses are positive, refunds are negative) so the system naturally handles all cases without special casing. **Default seed data fires once**: on first startup against an empty `categories` table, the system seeds Groceries, Dining, Transport, and other standard categories with pre-loaded keywords. This fires exactly once; it never merges or overwrites. To change what gets seeded, edit `src/main/java/com/bankcategorizer/config/DefaultCategorySeedData.java`.

## Next Steps

For running and building the project, see [docs/COMMANDS.md](COMMANDS.md). For the full data model and domain flow, see [docs/ARCHITECTURE.md](ARCHITECTURE.md). For quick reference on non-negotiables and gotchas, see [CLAUDE.md](../CLAUDE.md) at the project root.
