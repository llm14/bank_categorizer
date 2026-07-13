# Backlog

Themes and open issues to not forget — things to check again later. Not user stories (see `USER_STORIES.md` for those).

- Smart lookup for unrecognized merchants: when a transaction can't be matched by the seeded/user keyword list, let the user trigger a "search" for that specific transaction to get a suggested category (on top of manual categorization from US-4). Backend undecided — candidates include an LLM-based guess or an external merchant/business lookup API. Revisit once the app is more mature and the common-merchant seed list already in place is proving insufficient.
- Check whether an MCP server would actually be useful for this project's own workflow (as distinct from the account-level MCP connections already available in-session, e.g. Microsoft Learn/Pulsar) — candidates worth weighing: a Postgres MCP server for direct DB inspection during development, or a GitHub MCP server versus the `gh` CLI already used by `git-commit-pr-workflow`. Revisit once there's a concrete recurring task an MCP server would clearly do better than the current CLI/agent-based workflow, not just because it's available.
