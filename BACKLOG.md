# Backlog

Themes and open issues to not forget — things to check again later. Not user stories (see `USER_STORIES.md` for those).

- Re-check the `e2e-verifier` agent — it's still not working as expected.
- Smart lookup for unrecognized merchants: when a transaction can't be matched by the seeded/user keyword list, let the user trigger a "search" for that specific transaction to get a suggested category (on top of manual categorization from US-4). Backend undecided — candidates include an LLM-based guess or an external merchant/business lookup API. Revisit once the app is more mature and the common-merchant seed list already in place is proving insufficient.
- Review the `git-commit-pr-workflow` agent so running the full unit test suite before opening a PR is mandatory, not optional — and so it runs without prompting for confirmation.
- Deployment readiness (target: Docker), per the architecture-reviewer's deployment-focused pass — not yet done, revisit next:
  - Add Flyway + a baseline migration matching the current entity model. This is the actual hard blocker: `prod`'s `ddl-auto=validate` means a fresh prod database has no schema and the app can't start at all.
  - Add a multi-stage `Dockerfile` (build with Maven, run on a JRE 25 base image) plus a `docker-compose.yml` wiring the app + a Postgres container together.
  - Add `spring-boot-starter-actuator` and expose `/actuator/health` (with DB connectivity check) in prod — needed for Docker healthchecks/orchestrators to know the app is actually up.
  - Configure `spring.servlet.multipart.max-file-size`/`max-request-size` (default 1MB is too small for a real bank statement) with a proper 413/400 error instead of a generic 500; also fix `README.md`'s stale `application.properties` reference (should be `application.yml`) and document `prod`'s required env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) including `DB_URL`'s expected JDBC URL format.
  - Lower priority, no immediate action: CORS isn't configured (fine until there's a browser frontend, but note the gap), and there's no `mvnw`/Maven wrapper (Java version is pinned, Maven isn't).
