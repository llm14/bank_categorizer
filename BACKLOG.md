# Backlog

Themes and open issues to not forget — things to check again later. Not user stories (see `USER_STORIES.md` for those).

- Re-check the `e2e-verifier` agent — it's still not working as expected.
- Smart lookup for unrecognized merchants: when a transaction can't be matched by the seeded/user keyword list, let the user trigger a "search" for that specific transaction to get a suggested category (on top of manual categorization from US-4). Backend undecided — candidates include an LLM-based guess or an external merchant/business lookup API. Revisit once the app is more mature and the common-merchant seed list already in place is proving insufficient.
- Review the `git-commit-pr-workflow` agent so running the full unit test suite before opening a PR is mandatory, not optional — and so it runs without prompting for confirmation.
- Create an architecture agent: a specialized subagent dedicated to reviewing/guiding overall system architecture and design decisions (as distinct from `java-springboot-expert`, which implements code), for use as the project grows beyond a single-module backend.
