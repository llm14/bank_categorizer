# Backlog

Themes and open issues to not forget — things to check again later. Not user stories (see `USER_STORIES.md` for those).

- Smart lookup for unrecognized merchants: when a transaction can't be matched by the seeded/user keyword list, let the user trigger a "search" for that specific transaction to get a suggested category (on top of manual categorization from US-4). Backend undecided — candidates include an LLM-based guess or an external merchant/business lookup API. Revisit once the app is more mature and the common-merchant seed list already in place is proving insufficient.
- Try installing/using a Claude Code skill for frontend design work (e.g. an Anthropic frontend-design skill, if one is available) as a test of whether skill-based guidance measurably improves the visual quality/consistency of screens built by `react-frontend-expert` versus its current plain Tailwind conventions. Revisit next time a new frontend screen is being built (e.g. FE-8) — a good, low-risk opportunity to try it and compare.
