---
name: agent-audit
description: Audits .claude/agents/*.md definitions for internal inconsistencies — description/tool mismatches, stale cross-references, frontmatter drift — and reports findings without editing anything.
---

# Agent Definition Audit

You are reviewing this project's own subagent definitions (`.claude/agents/*.md`) for internal consistency problems — not code, not runtime behavior, just whether the definitions are coherent with each other and with what they claim to do. This is a read-only audit: report findings, never edit an agent file as part of this skill.

## Load the full set first

Read every file in `.claude/agents/*.md` in full before judging any single one — most of what you're checking is inconsistency *between* files, not badness of any one file in isolation.

## What to check

**Frontmatter consistency**
- `name:` matches the filename (kebab-case, e.g. `architecture-reviewer.md` → `name: architecture-reviewer`). Flag mismatches like Title Case or a name that diverges from the file it lives in.
- `model:` values use one consistent format across agents that pin a model (e.g. don't let one agent write `claude-haiku-4-5` and another `claude-haiku-4-5-20251001` for what's meant to be the same model).
- `tools:` is present and scoped to what the agent's own instructions say it does. An agent whose prose says "you never edit, write, or delete a file" but whose `tools:` line includes `Write`/`Edit` is a real contradiction — flag it immediately, in either direction (a tool it clearly needs but lacks, or one its own text disclaims but still has).
- `permissionMode: bypassPermissions` is paired with an explicit "hard limits regardless" section in the body (this project's established pattern for `e2e-verifier` and `git-commit-pr-workflow`) — an agent bypassing permissions with no stated limits is a gap.

**Cross-reference integrity**
- Every "Do not use for X — that's `other-agent`'s job" reference names an agent that actually exists (check the filename/`name:`), and that target agent's own description doesn't contradict the claim (e.g. A says "B handles Y," B's description says it doesn't handle Y).
- Boundary sections that appear in both directions (e.g. `react-frontend-expert` ↔ `java-springboot-expert`, `infra-engineer` ↔ `java-springboot-expert`) should agree on where the line is drawn — flag if one side's "that's their job" doesn't match the other side's stated scope.

**Scope overlap**
- Two agents whose descriptions would both plausibly claim the same task (ambiguous ownership) — flag the specific overlapping phrase from each, not just "these seem similar."

**Stale references**
- File paths, doc sections, or other agents/skills named in the body actually exist (`docs/COMMANDS.md`, `CLAUDE.md`, other `.claude/agents/*.md` files, skills under `.claude/skills/`). A reference to a skill/agent/file that's been renamed or removed since is a real bug.

**Description quality**
- The `description:` field alone (what the routing model sees before reading the body) states clearly what the agent is for AND what it isn't for — an agent description with no "do not use for" clause is worth flagging if its sibling agents all have one, since that's the established pattern for keeping this roster's boundaries legible.

## What NOT to flag

- Stylistic differences that don't cause ambiguity (one agent's prose is terser than another's).
- Missing sections that were never part of this project's pattern to begin with — ground every finding in either an existing convention elsewhere in `.claude/agents/` or an actual functional contradiction, not generic best-practice.

## Report format

Prioritized, most-impactful first. Each finding:
- **File(s)** involved (with the specific field/line where possible).
- **What's inconsistent**, stated concretely — quote both sides of a contradiction rather than describing it abstractly.
- **Why it matters** — what breaks in practice (wrong agent gets picked, a claimed restriction isn't actually enforced by `tools:`, a dangling reference confuses whoever reads it next).
- **Suggested fix**, briefly — this skill doesn't edit files itself; leave the actual edit to the user or a follow-up.

Close with a one-line verdict: how many agents were reviewed, and whether the roster is internally consistent enough to trust as-is.
