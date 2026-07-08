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

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd bank_categorizer
```

### 2. Configure the database

Create a PostgreSQL database and set the connection details in `src/main/resources/application.properties` (or via environment variables):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bank_categorizer
spring.datasource.username=<your-username>
spring.datasource.password=<your-password>
```

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
<td>Uploads a CSV or XLSX bank statement, parses it into transactions, and auto-categorizes each row by matching keyword. Malformed rows are skipped rather than failing the whole import.</td>
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
<td><code>GET /api/v1/transactions</code></td>
<td>Lists transactions, optionally filtered to only uncategorized ones via <code>?category=uncategorized</code>.</td>
<td>—</td>
<td>

```json
[
  {
    "id": 10,
    "date": "2026-06-01",
    "description": "MERCADONA MADRID",
    "amount": -54.32,
    "categoryId": 1,
    "categoryName": "Groceries"
  }
]
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

- [ ] Set up Spring Boot project skeleton
- [ ] Define transaction and category data models
- [ ] Implement CSV/XLSX file upload and parsing
- [ ] Implement transaction categorization logic
- [ ] Implement spending query/comparison endpoints
- [ ] Add tests

## License

TBD
