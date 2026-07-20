# CLAUDE.md

A Spring Boot backend that ingests bank statements (CSV/XLSX), auto-categorizes transactions by keyword matching, and answers spending questions. Single-user, local app.

## Non-Negotiables

**Schema & Database**
- Flyway is the source of truth for schema. Hibernate only validates, never creates or alters (`ddl-auto=validate`).
- New schema changes → add `V{n}__description.sql` migration files, never edit entities and rely on Hibernate.
- Connection details (host, databases, credential overrides): see [docs/COMMANDS.md](docs/COMMANDS.md#prerequisites).

**Business Logic**
- Categorization: case-insensitive substring match of transaction description against category keywords. First matching category (by id/creation order) wins — no scoring.
- Manual transaction corrections never retroactively change auto-categorization behavior. Corrections don't "teach" the system.
- Spending totals use *signed* amounts, then report absolute value. Refunds assigned to spending categories don't inflate totals.

**Architecture**
- Layered: `controller` (thin) → `service` (all logic) → `repository` (data access), plus `model` (JPA entities), `dto` (request/response records), `config`, `exception`.
- One exception class per distinct validation concern, never reuse generic exceptions. `@RestControllerAdvice` maps all to consistent JSON + HTTP status.
- Every service/controller pair includes both `@WebMvcTest` (MockMvc, mocked services) and Mockito unit tests. Testcontainers integration test exercises the full import → categorize → spend flow.

**Testability**
- `Clock` is injected, never `LocalDate.now()` or `new Date()`. Tests fix "today" via `Clock.fixed(...)` for determinism.
- Default seed data (`DefaultCategorySeedData` + `DefaultCategorySeeder`) only fires on empty `categories` table. Never merges or overwrites.

## Development Workflow

**Verification**
- Never claim code is "built and working" or tests pass without running them. Always verify by execution using the exact commands in [docs/COMMANDS.md](docs/COMMANDS.md) — test suite, then start the dev server and drive the API directly. For every task marked complete, show the exact command run and its output as proof — don't say something works unless you've run it.
- Do not introduce settings keys, config fields, or API methods without confirming they exist. Check `application.yml`, Javadoc, or existing usage before assuming an API surface.
- Before marking a task complete, run `/simplify` on the new/changed code to catch reuse, over-engineering, and altitude issues before they land.

**Testing & APIs**
- Use mocks or dry-runs by default in dev/test environments. Never default to real paid API calls without explicit authorization.
- Wire sandbox/mock APIs into the default config. Only switch to production credentials when explicitly instructed.

**Config Changes**
- When changing any `.claude/` setting or project-local agent config, state explicitly whether a session restart is needed before it takes effect. If so, don't attempt to test it within the current session — the change won't be loaded yet.

## Gotchas

- Period comparison only supports `period=month`. Other granularities are rejected (400), not implemented.
- Deleting a category uncategorizes its transactions (nulls `category_id`), doesn't cascade-delete.
- If a pre-existing database exists before Flyway was introduced (no `flyway_schema_history`), `spring.flyway.baseline-on-migrate=true` baselines at version 1 instead of refusing to run.

## Quick Links

- **Commands** (build, run, test): See [docs/COMMANDS.md](docs/COMMANDS.md)
- **Architecture & Domain Flow**: See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **Database Migrations**: `src/main/resources/db/migration/`
- **Default Categories**: `src/main/java/com/bankcategorizer/config/DefaultCategorySeedData.java`
