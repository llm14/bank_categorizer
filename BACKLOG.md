# Backlog

Themes and open issues to not forget — things to check again later. Not user stories (see `USER_STORIES.md` for those).

- Re-check the `e2e-verifier` agent — it's still not working as expected.
- Smart lookup for unrecognized merchants: when a transaction can't be matched by the seeded/user keyword list, let the user trigger a "search" for that specific transaction to get a suggested category (on top of manual categorization from US-4). Backend undecided — candidates include an LLM-based guess or an external merchant/business lookup API. Revisit once the app is more mature and the common-merchant seed list already in place is proving insufficient.
- Review the `git-commit-pr-workflow` agent so running the full unit test suite before opening a PR is mandatory, not optional — and so it runs without prompting for confirmation.
- Deployment readiness (target: Docker) is now scoped as US-7 through US-10 in `USER_STORIES.md`. Two smaller items from that same review weren't promoted to stories — no immediate action, just tracked here: CORS isn't configured (fine until there's a browser frontend, but note the gap), and there's no `mvnw`/Maven wrapper (Java version is pinned, Maven isn't).
- Check whether our project agents (`java-springboot-expert`, `e2e-verifier`, `git-commit-pr-workflow`, `architecture-reviewer`) need additional instructions or dedicated skills — e.g. gaps found in practice, repeated context we keep re-explaining per invocation, or missing Claude Code skills that would let an agent work more reliably/autonomously.
