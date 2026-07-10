# Backlog

Themes and open issues to not forget — things to check again later. Not user stories (see `USER_STORIES.md` for those).

- Smart lookup for unrecognized merchants: when a transaction can't be matched by the seeded/user keyword list, let the user trigger a "search" for that specific transaction to get a suggested category (on top of manual categorization from US-4). Backend undecided — candidates include an LLM-based guess or an external merchant/business lookup API. Revisit once the app is more mature and the common-merchant seed list already in place is proving insufficient.
- Deployment readiness (target: Docker) is now scoped as US-7 through US-10 in `USER_STORIES.md`. One smaller item from that same review wasn't promoted to a story — no immediate action, just tracked here: there's no `mvnw`/Maven wrapper (Java version is pinned, Maven isn't). The other (CORS not configured) is now scoped as FE-1, since it's no longer hypothetical now that a frontend is planned.
