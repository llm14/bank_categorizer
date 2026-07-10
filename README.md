# Bank Categorizer

A backend application that ingests a user's bank account movement list (CSV or Excel export) and automatically categorizes each transaction (e.g. groceries, rent, utilities, entertainment). Once categorized, the app can answer basic spending questions such as "How much did I spend on groceries compared to the last three months?".

## Features

- Import bank transactions from CSV or XLSX files
- Automatically categorize transactions (e.g. groceries, transport, utilities, entertainment)
- Query and compare spending across categories and time periods
- Persist transactions and categories in a relational database

## Tech Stack

- **Language:** Java 25 (LTS)
- **Framework:** Spring Boot 3.x
- **Build tool:** Maven
- **Database:** PostgreSQL

## Prerequisites

- JDK 25+
- Maven 3.9+
- PostgreSQL 16+ (running locally or via Docker)
- Node.js LTS + npm (only needed for `frontend/` — see "Frontend (development)" below)

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd bank_categorizer
```

### 2. Configure the database

Connection details are defined per Spring profile in `src/main/resources/application.yml`
(active profile defaults to `dev`, overridable via `SPRING_PROFILES_ACTIVE`).

The `dev` and `test` profiles connect to a fixed local host/port/database-name pattern
(`localhost:5432/bank_categorizer` and `localhost:5432/bank_categorizer_test`
respectively) and default `DB_USERNAME`/`DB_PASSWORD` to `postgres`/`postgres` if not
set — just create the corresponding local database(s) and, if your credentials
differ from the defaults, set the environment variables:

```bash
export DB_USERNAME=<your-username>
export DB_PASSWORD=<your-password>
```

`dev` and `test` also default the CORS-allowed frontend origin to the Vite dev
server's default origin if `FRONTEND_ORIGIN` isn't set:

| Variable | Purpose | Default |
|---|---|---|
| `FRONTEND_ORIGIN` | Origin allowed to make cross-origin requests to `/api/v1/**` and `/actuator/health` | `http://localhost:5173` |

The `prod` profile has no defaults and requires all of the following
environment variables to be set:

| Variable | Purpose | Example |
|---|---|---|
| `DB_URL` | Full JDBC URL (not just a host/port) | `jdbc:postgresql://host:5432/dbname` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `FRONTEND_ORIGIN` | Origin allowed to make cross-origin requests to `/api/v1/**` and `/actuator/health` | `https://app.example.com` |

### 3. Build the project

```bash
mvn clean install
```

### 4. Run the application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080` by default.

## Running Tests

```bash
mvn test
```

## Running via Docker

For a reproducible setup without installing a JDK, Node, or PostgreSQL locally, use
the multi-stage `Dockerfile`s and `docker-compose.yml` in the repo root. Compose
brings up the app (built from source with Maven, run on a JRE 25 base image), the
frontend (built with Node, served by Nginx), and a PostgreSQL 16 container, wired
via environment variables — no credentials are hardcoded.

### 1. Configure environment variables (optional)

Sensible local-dev defaults are baked into `docker-compose.yml`, so this step can be
skipped for local use. To override them, copy `.env.example` to `.env` (gitignored)
and adjust as needed:

```bash
cp .env.example .env
```

| Variable | Purpose | Default |
|---|---|---|
| `POSTGRES_DB` | Database name | `bank_categorizer` |
| `POSTGRES_USER` | Database user | `postgres` |
| `POSTGRES_PASSWORD` | Database password | `postgres` |
| `DB_HOST_PORT` | Host-side port mapped to Postgres' `5432` | `5433` (chosen because `5432` may already be taken by another local Postgres instance/container) |
| `APP_PORT` | Host-side port mapped to the app's `8080` | `8080` |
| `FRONTEND_PORT` | Host-side port mapped to the frontend's Nginx `80` | `3000` |
| `FRONTEND_ORIGIN` | Origin the app allows CORS requests from — must match wherever the frontend is actually reachable | `http://localhost:${FRONTEND_PORT:-3000}` |

### 2. Build and run

```bash
docker compose up --build
```

This builds the app and frontend images, starts Postgres, waits for it to report
healthy (`pg_isready`), then starts the app with `SPRING_PROFILES_ACTIVE=prod`, and
finally starts the frontend once the app itself reports healthy. On startup Flyway
creates the schema on the fresh database and the app seeds its default categories
automatically — no manual migration step needed.

The app itself also has a container-level healthcheck, backed by
`GET /actuator/health`: it returns `200`/`UP` when the database is reachable and
`503`/`DOWN` (or times out) otherwise, which `docker compose ps` surfaces as
`healthy`/`unhealthy`/`starting`. The runtime image has neither `curl` nor `wget`,
so the check is a plain HTTP request over bash's `/dev/tcp` instead of an
installed HTTP client. Give it a little time after startup (`start_period: 40s`)
for the Spring context and Flyway migrations to finish before it can respond.

The app is reachable at `http://localhost:8080` (or `$APP_PORT` if overridden), e.g.:

```bash
curl http://localhost:8080/api/v1/categories
curl http://localhost:8080/actuator/health
```

The frontend (an Nginx-served static build of `frontend/`) is reachable at
`http://localhost:3000` (or `$FRONTEND_PORT` if overridden) once `docker compose up`
reports it healthy — the Vite build bakes in `VITE_API_BASE_URL` as
`http://localhost:$APP_PORT` at *build* time (not runtime), since the browser calls
the backend directly rather than through the Docker network, so rebuild the
`frontend` image (`docker compose up --build frontend`) if you change `APP_PORT`.

Postgres data persists in a named volume (`db_data`) across `docker compose down`
restarts; use `docker compose down -v` to also wipe the database.

To stop everything:

```bash
docker compose down
```

## Frontend (development)

A React + TypeScript SPA (Vite, Tailwind CSS, TanStack Query) lives under `frontend/`
and gives the API above a browser UI (see `USER_STORIES.md`'s "Stage 5 — Frontend").
It's a separate app from the Java backend, run independently in development:

```bash
cd frontend
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` by default (Vite's default port) — the
backend's CORS config (`FRONTEND_ORIGIN`, see above) already allows this origin by
default in the `dev`/`test` profiles, so no backend changes are needed to develop
against it locally.

The frontend talks to the backend via the `VITE_API_BASE_URL` env var (never
hardcoded), defaulting to `http://localhost:8080`. Copy `frontend/.env.example` to
`frontend/.env` (gitignored) to override it:

```bash
cp frontend/.env.example frontend/.env
```

Other useful commands (run from `frontend/`):

```bash
npm run build      # type-checks and produces a production build in frontend/dist
npm test           # runs the Vitest + React Testing Library suite
npm run lint       # ESLint
npm run typecheck  # tsc, no emit
```

For a containerized, production-like build instead of `npm run dev`, see the
"Running via Docker" section above — `docker compose up --build` also builds and
serves `frontend/` via Nginx as its own `frontend` service.

## Project Structure

```
bank_categorizer/
├── src/
│   ├── main/
│   │   ├── java/          # Application source code
│   │   └── resources/     # Configuration files
│   └── test/               # Unit and integration tests
├── pom.xml
└── README.md
```

## API Reference

<table>
<thead>
<tr>
<th>API path</th>
<th>Description</th>
<th>Payload example</th>
<th>Response example</th>
</tr>
</thead>
<tbody>

<tr>
<td><code>POST /api/v1/categories</code></td>
<td>Creates a new spending category with optional matching keywords. Fails with <code>409</code> if a category with the same name already exists.</td>
<td>

```json
{
  "name": "Groceries",
  "description": "Supermarkets and grocery stores",
  "keywords": ["MERCADONA", "LIDL"]
}
```

</td>
<td>

```json
{
  "id": 1,
  "name": "Groceries",
  "description": "Supermarkets and grocery stores",
  "keywords": ["MERCADONA", "LIDL"]
}
```

</td>
</tr>

<tr>
<td><code>GET /api/v1/categories</code></td>
<td>Lists all spending categories, including their keywords.</td>
<td>—</td>
<td>

```json
[
  {
    "id": 1,
    "name": "Groceries",
    "description": "Supermarkets and grocery stores",
    "keywords": ["MERCADONA", "LIDL"]
  }
]
```

</td>
</tr>

<tr>
<td><code>DELETE /api/v1/categories/{id}</code></td>
<td>Deletes a category. Transactions referencing it become uncategorized rather than being deleted.</td>
<td>—</td>
<td><code>204 No Content</code> (empty body)</td>
</tr>

<tr>
<td><code>POST /api/v1/transactions/import</code></td>
<td>Uploads a CSV or XLSX bank statement, parses it into transactions, and auto-categorizes each row by matching keyword. Malformed rows are skipped rather than failing the whole import. Uploads larger than 15MB are rejected with <code>413 Payload Too Large</code>.</td>
<td>

`multipart/form-data` with a `file` field (e.g. `transactions.csv`)

</td>
<td>

```json
{
  "fileName": "transactions.csv",
  "totalRows": 5,
  "importedCount": 5,
  "skippedCount": 0,
  "categorizedCount": 3,
  "uncategorizedCount": 2
}
```

</td>
</tr>

<tr>
<td><code>GET /api/v1/transactions?category={uncategorized}&page={n}&size={n}&sort={field,dir}</code></td>
<td>Lists transactions page by page (defaults: page 0, size 20, sorted by date descending), optionally filtered to only uncategorized ones via <code>?category=uncategorized</code>.</td>
<td>—</td>
<td>

```json
{
  "content": [
    {
      "id": 10,
      "date": "2026-06-01",
      "description": "MERCADONA MADRID",
      "amount": -54.32,
      "categoryId": 1,
      "categoryName": "Groceries"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

</td>
</tr>

<tr>
<td><code>PATCH /api/v1/transactions/{id}</code></td>
<td>Manually assigns or corrects a transaction's category. Does not alter the category's keywords, so it never retroactively changes auto-categorization rules.</td>
<td>

```json
{
  "categoryId": 3
}
```

</td>
<td>

```json
{
  "id": 10,
  "date": "2026-06-01",
  "description": "MERCADONA MADRID",
  "amount": -54.32,
  "categoryId": 3,
  "categoryName": "Transport"
}
```

</td>
</tr>

<tr>
<td><code>GET /api/v1/spending?category={id}&from={date}&to={date}</code></td>
<td>Returns the total spent in a category over a date range. Omitting <code>category</code> instead returns a breakdown of totals across every category with activity in that range.</td>
<td>—</td>
<td>

```json
{
  "categoryId": 1,
  "categoryName": "Groceries",
  "from": "2026-06-01",
  "to": "2026-06-30",
  "totalSpent": 245.67
}
```

</td>
</tr>

<tr>
<td><code>GET /api/v1/spending/compare?category={id}&period=month&lookback={n}</code></td>
<td>Compares a category's spending in the current month against each of the previous <code>lookback</code> months, plus their average. Only <code>period=month</code> is currently supported.</td>
<td>—</td>
<td>

```json
{
  "categoryId": 1,
  "categoryName": "Groceries",
  "period": "month",
  "lookback": 3,
  "current": {
    "label": "2026-07",
    "from": "2026-07-01",
    "to": "2026-07-31",
    "totalSpent": 180.20
  },
  "previousPeriods": [
    { "label": "2026-06", "from": "2026-06-01", "to": "2026-06-30", "totalSpent": 245.67 },
    { "label": "2026-05", "from": "2026-05-01", "to": "2026-05-31", "totalSpent": 210.00 },
    { "label": "2026-04", "from": "2026-04-01", "to": "2026-04-30", "totalSpent": 195.50 }
  ],
  "previousAverage": 217.06
}
```

</td>
</tr>

</tbody>
</table>

## Roadmap

- [x] Stage 1 — Import bank statements (CSV/XLSX upload and parsing)
- [x] Stage 2 — Categorization (auto-categorization by keyword, manual (re)categorization)
- [x] Stage 3 — Spending questions (totals by category, period-over-period comparison)
- [x] Stage 4 — Deployment readiness (Flyway migrations, Docker/docker-compose, health checks, production-ready file uploads)
- [ ] Stage 5 — Frontend (React/TypeScript UI over the existing API)

See `USER_STORIES.md` for the detailed, acceptance-criteria-level spec behind each stage.

## License

TBD
