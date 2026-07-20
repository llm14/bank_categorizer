# User Stories

Concrete, acceptance-criteria-level specs for each unit of work, complementing the high-level roadmap in `README.md`. Each story is small enough to map to one branch/PR.

Status legend: ✅ Done · 🔜 Planned

---

## Stage 1 — Import (done)

### US-1: Upload a bank statement ✅
As a user, I want to upload my bank statement as a CSV or XLSX file so that my transactions are recorded in the app without manual entry.

**Acceptance criteria:**
- `POST /api/v1/transactions/import` accepts a multipart file (CSV or XLSX).
- Rows are parsed into transactions (date, description, amount) and saved, uncategorized.
- Malformed rows are skipped, not fatal — the response reports how many rows were imported vs. skipped.
- Unsupported file types return a clear 400 error.

---

## Stage 2 — Categorization (done)

### US-2: Manage categories ✅
As a user, I want to create, list, and delete spending categories (e.g. Groceries, Rent, Transport) so that I can organize my transactions the way I think about my spending.

**Acceptance criteria:**
- `POST /api/v1/categories` creates a category (name required, unique).
- `GET /api/v1/categories` lists all categories.
- `DELETE /api/v1/categories/{id}` deletes a category; transactions referencing it become uncategorized rather than being deleted.

### US-3: Auto-categorize imported transactions ✅
As a user, I want newly imported transactions to be automatically assigned a category based on their description so that I don't have to categorize everything by hand.

**Acceptance criteria:**
- Each category can have one or more matching rules (e.g. keyword/substring match against the transaction description, case-insensitive).
- On import, each parsed transaction is matched against existing category rules; the first/best match is assigned.
- Transactions with no matching rule are saved as uncategorized (existing behavior, unchanged).
- Import response includes how many transactions were auto-categorized vs. left uncategorized.
- On first run (empty category table), the app seeds a default set of common categories with well-known merchant keywords, so out-of-the-box categorization works without manual setup; this seeding never overwrites or merges into an existing (non-empty) category set.

### US-4: Manually (re)categorize a transaction ✅
As a user, I want to view my uncategorized transactions and assign or correct a category myself so that I can fix mistakes the automatic categorization makes.

**Acceptance criteria:**
- `GET /api/v1/transactions?category=uncategorized` (or similar filter) lists uncategorized transactions.
- `PATCH /api/v1/transactions/{id}` updates a transaction's category.
- Manually correcting a transaction does not retroactively change the category rules from US-3 (no implicit "learning" yet — that's a future story if wanted).

---

## Stage 3 — Spending questions (done)

### US-5: Spending total by category over a period ✅
As a user, I want to see how much I spent in a given category over a given time period so that I can understand my spending in that area.

**Acceptance criteria:**
- `GET /api/v1/spending?category={id}&from={date}&to={date}` returns the total amount spent in that category within the range.
- Omitting `category` returns totals broken down by category for the range.

### US-6: Compare spending across periods ✅
As a user, I want to compare how much I spent in a category this period versus previous periods (e.g. "groceries this month vs. the average of the last 3 months") so that I can spot trends in my spending.

**Acceptance criteria:**
- `GET /api/v1/spending/compare?category={id}&period=month&lookback=3` returns the current period's total alongside the total (and/or average) for each of the previous N periods.
- Works for at least "month" as a period granularity; other granularities (week/year) are a future extension, not required now.
- Uncategorized transactions are excluded from category-based comparisons.

---

## Stage 4 — Deployment readiness (planned)

Scoped from the deployment-readiness findings tracked in `BACKLOG.md`. Target deployment shape: Docker.

### US-7: Schema migrations via Flyway ✅
As a developer, I want the database schema created and versioned via migrations instead of relying on Hibernate's `ddl-auto`, so that the app can actually start against a fresh production database.

**Acceptance criteria:**
- Flyway is added as a dependency, with a baseline migration matching the current entity model exactly.
- The `prod` profile keeps `ddl-auto=validate` (schema is now provided by Flyway, not Hibernate); migrations run automatically on startup before the app accepts traffic.
- A fresh, empty PostgreSQL database can start the app successfully end-to-end — this is the actual current blocker: `validate` mode never creates schema, so today a fresh prod database can't start the app at all.
- Future schema changes are made via new migration files rather than depending on `ddl-auto=update`.

### US-8: Containerize the app for Docker deployment ✅
As the person deploying this app, I want a Dockerfile and a docker-compose setup so that I can run the app and its database with a single, reproducible command instead of manual setup.

**Acceptance criteria:**
- A multi-stage `Dockerfile` builds the app with Maven and runs it on a JRE 25 base image.
- A `docker-compose.yml` wires the app together with a PostgreSQL container, using environment variables (not hardcoded values) for credentials/connection details.
- `docker compose up` brings up a working app reachable on its configured port, against a freshly-migrated database (depends on US-7).
- `README.md` documents how to build and run the app via Docker.

### US-9: Health check endpoint ✅
As the person operating this app (e.g. via a Docker healthcheck or a load balancer), I want a health endpoint so that I can tell whether the app and its database connection are actually up.

**Acceptance criteria:**
- `spring-boot-starter-actuator` is added as a dependency.
- `/actuator/health` is exposed in the `prod` profile and reflects real database connectivity, not just JVM liveness.
- The Docker setup from US-8 wires this endpoint into a container healthcheck.

### US-10: Production-ready file uploads and deployment docs ✅
As a user uploading a real bank statement in a deployed environment, I want the app to accept realistically-sized files with a clear error when something's wrong, so my first real upload doesn't fail with a confusing generic error.

**Acceptance criteria:**
- `spring.servlet.multipart.max-file-size`/`max-request-size` are explicitly configured (e.g. 10–20MB) instead of relying on Spring Boot's 1MB/10MB defaults.
- Exceeding the limit returns a clear 400/413 error via `GlobalExceptionHandler`, not a generic 500.
- `README.md`'s configuration section is corrected (it currently references a nonexistent `application.properties` instead of `application.yml`) and documents the `prod` profile's required environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), including `DB_URL`'s expected JDBC URL format.

### US-11: Manually add a transaction ✅
As a user, I want to manually add a single transaction (not just via CSV/XLSX import) so that I can record something a statement never included, like a cash payment. This is the backend endpoint FE-9 (Stage 5) builds a form against.

**Acceptance criteria:**
- `POST /api/v1/transactions` accepts a date, description, and signed amount, creates a single transaction, and runs it through the same auto-categorization (`CategorizationService`) as an imported row before saving — consistent with import's behavior rather than a separate uncategorized-by-default path.
- Required-field validation (400 on a missing/invalid date, description, or amount) follows this project's existing Bean Validation + `GlobalExceptionHandler` convention.
- The created transaction follows the same signed-amount convention as everything else (expenses negative, income positive) — no special-case sign handling for manually-added rows.

### US-12: Period and comparison totals across all categories ✅
As a user, I want a total spent across *all* categories for whatever period I'm looking at, not just a per-category breakdown, so I get the "how much did I spend, full stop" answer without adding up the list myself. This is the backend counterpart FE-10 (Stage 5) surfaces in the Spending dashboard and Spending comparison screens.

**Acceptance criteria:**
- `GET /api/v1/spending` (category omitted, breakdown mode) also returns a total spent across every category included in that date range, alongside the existing per-category breakdown — computed once server-side rather than left for callers to sum themselves, consistent with this codebase's existing single-source-of-truth convention for aggregation (e.g. `SpendingComparisonService` delegating to `SpendingService`).
- `GET /api/v1/spending/compare`'s `category` param becomes optional (currently required): when omitted, it compares total spending across *all* categories for the current period against each of the previous `lookback` periods and their average, reusing the existing response shape (categoryId/categoryName null or omitted for an all-categories comparison).
- Existing single-category behavior for both endpoints is unchanged — this only adds the all-categories total as an option, not a replacement.
- Covered by tests the same way existing spending endpoints are (Mockito service tests + MockMvc slice tests), and the Postman collection is updated to document the new/changed response shapes.

### US-13: User login ✅
As a user, I want to log in before I can use the app so that my transactions and categories aren't reachable by anyone who can hit the API — this ends the app's current fully-open, no-auth state now that it has a real browser UI (previously tracked as "Authentication" in `BACKLOG.md`). This is the backend counterpart FE-11 (Stage 5) builds a login screen against.

**Acceptance criteria:**
- A single set of login credentials (username/password), configured via environment variables (matching this project's existing env-var-driven convention — no hardcoded default in `prod`, a dev-only default is fine) — this remains a single-user app, not multi-account/multi-tenant.
- A login endpoint issues a session (or token) on valid credentials; every existing `/api/v1/**` endpoint requires it, returning 401 for missing/invalid auth rather than serving data openly. `/actuator/health` stays reachable without auth (needed for container healthchecks).
- The existing CORS config (FE-1) is updated as needed for whichever mechanism is chosen (e.g. `allowCredentials` for a session cookie), still scoped to the configured `FRONTEND_ORIGIN`, never wildcarded.
- Covered by tests the same way existing endpoints are (slice/integration tests asserting 401 without auth and 200 with valid auth), and the Postman collection/README document how to authenticate a request.

### US-14: Detect already-imported transactions on re-upload 🔜
As a user, I want re-uploading a bank export that overlaps with a previous upload (e.g. downloading a fresh monthly CSV that includes some of last month's dates) to skip transactions already in the app, rather than creating duplicates or resetting their category, so I don't have to manually check dates before every upload.

**Acceptance criteria:**
- A transaction is considered a duplicate of an existing one when the parsed date, description, and amount all match exactly — there's no external transaction id in a CSV/XLSX export, so this triple is the natural identity for a statement line.
- On import, a duplicate row is skipped (not inserted again) and its existing category — whether auto-assigned or manually corrected via US-4/FE-5 — is left completely untouched; re-importing never resets a transaction back to a fresh auto-categorized or uncategorized state.
- `ImportResultResponse` reports a distinct duplicate count, separate from the existing malformed-row `skippedCount`, so a user can tell "this row was junk" apart from "this row was already here." (The existing upload screen, FE-3, already renders this summary and picks up the new count with a small display update — not a new screen.)
- Covered by tests the same way import is already tested (service unit tests + the Testcontainers-backed integration test), including a case that re-imports a file with partial overlap and confirms only the new rows are added.

---

## Stage 5 — Frontend (planned)

A React/TypeScript/Vite single-page app (`frontend/`) giving the existing REST API a browser UI: upload statements, manage categories, review/fix categorization, and answer spending questions, instead of driving everything via Postman/curl. Styled with Tailwind CSS, server state managed with TanStack Query. See the `react-frontend-expert` agent for the chosen stack's conventions.

### FE-1: Enable CORS for the frontend origin ✅
As a developer, I want the backend to accept cross-origin requests from the frontend so that a browser-based UI can actually call the API — promotes the CORS gap tracked in `BACKLOG.md` since it's no longer hypothetical once a frontend exists.

**Acceptance criteria:**
- CORS is configured (e.g. a `WebMvcConfigurer` bean) allowing the frontend's origin(s); the allowed origin(s) come from an env-var-driven config (dev default: the Vite dev server's origin; prod: configurable via env var), not hardcoded or wildcarded.
- Only the endpoints under `/api/v1/**` (and `/actuator/health`, needed by FE-2's connectivity check) allow cross-origin requests.
- Existing API behavior and tests are unaffected — this only adds cross-origin headers/preflight handling.

### FE-2: Frontend scaffolding & backend connectivity ✅
As a user, I want a working React app shell that can reach the backend so that the feature screens below (FE-3 through FE-7) have a real foundation to build on.

**Acceptance criteria:**
- `frontend/` scaffolded with Vite + React + TypeScript + Tailwind CSS.
- A typed API client (base URL from `VITE_API_BASE_URL`, not hardcoded) with typed request/response models matching the backend's DTOs (`CategoryResponse`, `TransactionResponse`, `PageResponse<T>`, `SpendingResponse`, `SpendingComparisonResponse`, `ErrorResponse`).
- TanStack Query's provider is wired at the app root for server-state fetching/caching.
- A minimal screen calls `/actuator/health` on load and shows connected/disconnected status, proving the CORS setup from FE-1 actually works end-to-end from a browser.
- `docker-compose.yml` gains a `frontend` service (built/served via Nginx) so `docker compose up` brings up frontend + backend + db together; `README.md` documents the new port.

### FE-3: Upload a bank statement ✅
As a user, I want to upload a CSV/XLSX statement from the browser so that I don't need Postman/curl to get my transactions in.

**Acceptance criteria:**
- An upload screen (file picker) posts to `POST /api/v1/transactions/import`.
- On success, shows the import summary (imported/skipped/categorized/uncategorized counts) from `ImportResultResponse`.
- Unsupported file type (400) and oversized file (413) errors are shown using the backend's actual `message`, not a generic failure string.

### FE-4: Manage categories ✅
As a user, I want to create, view, and delete categories from the browser so that I don't need the API directly for category setup (mirrors US-2).

**Acceptance criteria:**
- A categories screen lists all categories (`GET /api/v1/categories`), including keywords.
- A create-category form (name, description, keywords) posts to `POST /api/v1/categories`, showing field-level validation errors (400) and the duplicate-name case (409) inline.
- Deleting a category asks for confirmation and communicates that its transactions become uncategorized rather than being deleted, matching actual backend behavior.

### FE-5: Review and fix categorization ✅
As a user, I want to see my transactions, filter to the uncategorized ones, and correct a category from the browser (mirrors US-3/US-4).

**Acceptance criteria:**
- A paginated, sortable transaction table (`GET /api/v1/transactions`) shows date, description, signed amount, and category (or "Uncategorized").
- A filter toggle switches to uncategorized-only (`?category=uncategorized`).
- Each row has a category picker that calls `PATCH /api/v1/transactions/{id}`; the list reflects the change (via TanStack Query invalidation/refetch) without a full page reload.

### FE-6: Spending totals dashboard ✅
As a user, I want to see how much I spent by category over a date range from the browser (mirrors US-5).

**Acceptance criteria:**
- A date range picker (from/to) drives both views below.
- A breakdown view shows total spent per category for the range (`GET /api/v1/spending`); selecting one category shows just its total.
- Invalid ranges (400) and an unknown category (404) surface the backend's actual error message.

### FE-7: Compare spending across periods ✅
As a user, I want to compare a category's current-month spend against previous months from the browser (mirrors US-6).

**Acceptance criteria:**
- A category selector plus a `lookback` input (bounded 1–24, matching the backend's cap) calls `GET /api/v1/spending/compare`.
- Renders the current period's total alongside each previous period and their average (`SpendingComparisonResponse`).
- An out-of-range `lookback` or unsupported `period` shows the backend's actual 400 message rather than a generic error.

### FE-8: Landing page with section navigation ✅
As a user, I want a simple landing page with the app name and a button per feature area so that I can jump straight to the section I need instead of scrolling through every screen stacked on one page.

**Acceptance criteria:**
- The app's default view is a landing page showing just the app name (plain text, no functional content) and four buttons: "Manage categories", "Review transactions", "Spending dashboard", "Spending comparison".
- Clicking a button shows only that section's existing screen (`ManageCategories`/`ReviewTransactions`/`SpendingDashboard`/`SpendingComparison` from FE-4–FE-7); the landing page and the other three sections are hidden while one is active.
- A way to get back to the landing page from within a section (e.g. a "Back"/"Home" control), so the user isn't stuck once a section is open.
- Out of scope for this story: the upload-statement screen (FE-3) and the backend connectivity check (FE-2) aren't part of this button set — their current behavior/placement is left as-is unless a future story revisits it.

### FE-9: Manually add a transaction ✅
As a user, I want a form to manually add a single transaction so that I don't need Postman/curl to use the `POST /api/v1/transactions` endpoint from US-11.

**Acceptance criteria:**
- A form (date, description, amount) reachable from the "Review transactions" section posts to `POST /api/v1/transactions` (US-11).
- On success, the transaction list refreshes to show the new row without a full page reload.
- The form doesn't flip sign or apply any special-case handling on the amount — same signed-amount convention as everywhere else (expenses negative, income positive).
- A validation failure surfaces the backend's real error message, matching every other screen's error-handling convention.

### FE-10: Show period totals in the spending dashboard and comparison ✅
As a user, I want to see the overall total for my selected period directly on the Spending dashboard and Spending comparison screens, not just per-category numbers, using the all-categories totals from US-12.

**Note:** US-12's response shape change (`GET /api/v1/spending`'s no-category response moved from a bare `SpendingResponse[]` to `{ breakdown: SpendingResponse[], totalSpent: BigDecimal }`) had silently broken `SpendingDashboard.tsx` (FE-6) — already fixed separately (a `SpendingBreakdownResponse` type was added and the component updated to read `response.breakdown`). What's left for this story is purely additive: actually surfacing `totalSpent` in the UI and adding the all-categories comparison option.

**Acceptance criteria:**
- Spending dashboard: the all-categories breakdown view shows `response.totalSpent` alongside the existing per-category list.
- Spending comparison: an "All categories" option is added to the existing category selector (mirroring the dashboard's breakdown-vs-single-category pattern); selecting it calls `compareSpending` with `category` omitted and renders the same current/previous-periods/average layout already used for a single category, but for the combined all-categories total from US-12.
- Both reuse the existing typed API client/response handling patterns (`ApiError` message surfacing, TanStack Query) rather than introducing a parallel one-off fetch path.

### FE-11: Login screen ✅
As a user, I want a login screen so that I have to authenticate before reaching any part of the app, using the login endpoint from US-13.

**This is now urgent, not just the next story in sequence:** US-13 gated every `/api/v1/**` endpoint behind a bearer token, and `frontend/src/api/client.ts` currently attaches no `Authorization` header at all. As a result, **every screen built in FE-3 through FE-8 is currently non-functional against `main`** — each returns 401 (only `HealthStatus`, which hits the still-open `/actuator/health`, still works). This isn't a future integration concern; the app is broken end-to-end until this ships.

**Acceptance criteria:**
- A login form (username/password) is the only thing shown until authenticated — the landing page (FE-8) and every section are unreachable without logging in first. Calls `POST /api/v1/auth/login`.
- On success, store the returned bearer token (e.g. in memory/component state, or `sessionStorage` if it should survive a page refresh — your call) and update `client.ts`'s shared `request`/`requestFormData` helpers to attach `Authorization: Bearer <token>` to every subsequent request, so every existing screen (FE-3 through FE-10) works unchanged with no per-screen changes needed.
- A logout control calls `POST /api/v1/auth/logout`, clears the stored token, and returns to the login form.
- A failed login surfaces the backend's real error message (401, generic "Invalid username or password") — not a generic failure string. A 401 from any *other* endpoint (e.g. an expired token) should also drop the user back to the login form rather than showing a raw error on whatever screen they were on.

---

## Backlog / not yet scoped

- Editing/deleting individual transactions.
- Recurring-transaction detection.
- Multi-account support (currently assumes a single statement/account).
