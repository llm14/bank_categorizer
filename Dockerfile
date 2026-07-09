# --- Build stage: resolve dependencies and compile with Maven + JDK ---
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Copy only the POM first so dependency resolution is cached independently
# of source changes.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Now copy sources and build the jar (skip tests here; run them in CI/locally).
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Runtime stage: slim JRE-only image ---
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Run as a dedicated non-root user rather than the default root.
RUN groupadd --system app && useradd --system --gid app --home-dir /app app

COPY --from=build /build/target/*.jar app.jar
RUN chown app:app app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
