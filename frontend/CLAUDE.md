# CLAUDE.md (frontend/)

Guidance specific to this `frontend/` React app. See the repo root `CLAUDE.md` for the backend it talks to. Fuller conventions and cross-agent boundaries live in `.claude/agents/react-frontend-expert.md` — this file is the auto-loaded quick reference for anyone (or any session) working under `frontend/`, not a duplicate of that agent's full instructions.

## Project overview

A React + TypeScript single-page app (Vite, Tailwind CSS, TanStack Query) giving the backend's REST API a browser UI: upload statements, manage categories, review/fix categorization, and answer spending questions. See `USER_STORIES.md`'s "Stage 5 — Frontend" (repo root) for the stories this implements.

## Commands

Run from this directory (`frontend/`):

- Install: `npm install`
- Dev server: `npm run dev` (`http://localhost:5173` by default)
- Build: `npm run build`
- Test: `npm test`
- Lint: `npm run lint`
- Typecheck: `npm run typecheck`

Node LTS + npm are expected to be installed locally (see the repo root `README.md`'s Prerequisites) — no need to wrap these in Docker.

## Architecture

- `src/api/` — one typed function per backend endpoint (`categories.ts`, `transactions.ts`, `spending.ts`, `health.ts`), a shared `client.ts` (`request`/`requestFormData` helpers, the `ApiError` class), and `types.ts` (TypeScript mirrors of the backend's DTOs — keep field names/types in sync with the Java records, which are the source of truth).
- `src/components/` — one screen/component per feature, each with a colocated `*.test.tsx`: `LandingPage`, `UploadStatement`, `ManageCategories`, `ReviewTransactions`, `SpendingDashboard`, `SpendingComparison`, plus `HealthStatus` (the backend connectivity check).
- `src/App.tsx` — the app shell: a plain `activeSection` state machine (`null | "categories" | "transactions" | "dashboard" | "comparison"`) drives which section shows, with `LandingPage` as the default view. No routing library — deliberately not needed for four in-memory view toggles with no deep-linking requirement; revisit only if a future story genuinely needs shareable URLs per section.

## Conventions

- API base URL comes from the `VITE_API_BASE_URL` env var (see `.env.example`), never hardcoded — mirrors the backend's own env-var-driven config philosophy.
- Server state (anything from the API) goes through TanStack Query (`useQuery`/`useMutation`); local UI-only state (form inputs, toggles, filters) uses plain React state — don't reach for a global store for server data.
- Render the backend's actual error message (`ApiError.message`, sourced from the real `ErrorResponse` body) when something fails — never invent generic "something went wrong" text when the backend already gives a specific reason.
- Amounts display exactly as the backend returns them (signed: expenses negative, income positive) in transaction lists — don't flip signs or apply `.abs()` client-side. Spend-total screens (`SpendingDashboard`/`SpendingComparison`) already receive an absolute `totalSpent` from the backend; display it as given.
- Tests mock the typed API client (e.g. `../api/categories`), not `fetch` directly, so they assert on behavior rather than transport details. Every screen's loading/empty/error states are tested alongside the happy path.
- Styling is plain Tailwind, consistent across screens. An Anthropic `frontend-design` skill is installed (`.claude/skills/frontend-design/`) for genuine visual-design/polish asks, but it pushes toward bold one-off looks — it was trialed once on `LandingPage` and now sits as a deliberate stylistic outlier next to the other four screens' plain look (see `BACKLOG.md`). Don't reach for it on a new functional screen by default.
