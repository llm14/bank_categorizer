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

## Stage 2 — Categorization (planned)

### US-2: Manage categories 🔜
As a user, I want to create, list, and delete spending categories (e.g. Groceries, Rent, Transport) so that I can organize my transactions the way I think about my spending.

**Acceptance criteria:**
- `POST /api/v1/categories` creates a category (name required, unique).
- `GET /api/v1/categories` lists all categories.
- `DELETE /api/v1/categories/{id}` deletes a category; transactions referencing it become uncategorized rather than being deleted.

### US-3: Auto-categorize imported transactions 🔜
As a user, I want newly imported transactions to be automatically assigned a category based on their description so that I don't have to categorize everything by hand.

**Acceptance criteria:**
- Each category can have one or more matching rules (e.g. keyword/substring match against the transaction description, case-insensitive).
- On import, each parsed transaction is matched against existing category rules; the first/best match is assigned.
- Transactions with no matching rule are saved as uncategorized (existing behavior, unchanged).
- Import response includes how many transactions were auto-categorized vs. left uncategorized.
- On first run (empty category table), the app seeds a default set of common categories with well-known merchant keywords, so out-of-the-box categorization works without manual setup; this seeding never overwrites or merges into an existing (non-empty) category set.

### US-4: Manually (re)categorize a transaction 🔜
As a user, I want to view my uncategorized transactions and assign or correct a category myself so that I can fix mistakes the automatic categorization makes.

**Acceptance criteria:**
- `GET /api/v1/transactions?category=uncategorized` (or similar filter) lists uncategorized transactions.
- `PATCH /api/v1/transactions/{id}` updates a transaction's category.
- Manually correcting a transaction does not retroactively change the category rules from US-3 (no implicit "learning" yet — that's a future story if wanted).

---

## Stage 3 — Spending questions (planned)

### US-5: Spending total by category over a period 🔜
As a user, I want to see how much I spent in a given category over a given time period so that I can understand my spending in that area.

**Acceptance criteria:**
- `GET /api/v1/spending?category={id}&from={date}&to={date}` returns the total amount spent in that category within the range.
- Omitting `category` returns totals broken down by category for the range.

### US-6: Compare spending across periods 🔜
As a user, I want to compare how much I spent in a category this period versus previous periods (e.g. "groceries this month vs. the average of the last 3 months") so that I can spot trends in my spending.

**Acceptance criteria:**
- `GET /api/v1/spending/compare?category={id}&period=month&lookback=3` returns the current period's total alongside the total (and/or average) for each of the previous N periods.
- Works for at least "month" as a period granularity; other granularities (week/year) are a future extension, not required now.
- Uncategorized transactions are excluded from category-based comparisons.

---

## Backlog / not yet scoped

- Editing/deleting individual transactions.
- Recurring-transaction detection.
- Multi-account support (currently assumes a single statement/account).
- Authentication (currently a single-user, local app).
