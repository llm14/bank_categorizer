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

## Roadmap

- [ ] Set up Spring Boot project skeleton
- [ ] Define transaction and category data models
- [ ] Implement CSV/XLSX file upload and parsing
- [ ] Implement transaction categorization logic
- [ ] Implement spending query/comparison endpoints
- [ ] Add tests

## License

TBD
