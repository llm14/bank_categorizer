# Commands

This project uses the Maven Wrapper (`mvnw` / `mvnw.cmd`), pinned to a fixed Maven version. Use it instead of a globally installed `mvn`.

## Build & Run

```bash
# Build (clean compile + package)
./mvnw clean install

# Run locally (starts on http://localhost:8080, dev profile active)
./mvnw spring-boot:run
```

## Testing

```bash
# Full test suite
./mvnw test

# Single test class
./mvnw test -Dtest=ClassName

# Single test method
./mvnw test -Dtest=ClassName#methodName
```

## Prerequisites

- **PostgreSQL**: Running on `localhost:5432`
  - Database: `bank_categorizer` (dev profile)
  - Database: `bank_categorizer_test` (test profile)
  - Override with `DB_USERNAME` / `DB_PASSWORD` environment variables
- **Java 25**
- **Maven 3.9+** (provided via wrapper)

Schema is managed entirely by Flyway and created automatically on startup.
