# Agent Eval Notes

Manual eval log for the agents under `.claude/agents/`, per `USER_STORIES.md` US-15. Each run: seed a known issue, invoke the agent against it, grade the report against a short rubric, record the result here — pass or fail — so drift in an agent's behavior over time is visible instead of relying on a one-off gut check.

---

## architecture-reviewer

### Run 1 — 2026-07-28

**Fixture:** Seeded a missing `GlobalExceptionHandler` mapping. Added `InvalidTransactionAmountException` (new exception class, same shape as sibling validation exceptions), thrown from `TransactionService.create()` when a transaction's amount is exactly zero, deliberately left unmapped in `GlobalExceptionHandler` (so it falls through to the generic 500 handler instead of a 400). Seeded in an isolated git worktree on a scratch branch (`eval/us-15-fixture`), reviewed via `git diff main...HEAD`, then discarded — never merged.

**Rubric:**

| Criterion | Result |
|---|---|
| Seeded issue flagged with a correct file:line reference | ✅ Pass — cited `TransactionService.java:56-58` (throw is on line 57) and cross-checked `GlobalExceptionHandler.java` to confirm no handler exists |
| Report follows its documented format (file:line, what's there, why it matters, suggested fix) | ✅ Pass — matched exactly, plus the required closing verdict |
| No fabricated findings outside the seeded issue's actual scope | ✅ Pass — the one extra finding (no test covers the new branch) is real and directly tied to the same change, not invented |

**Verdict:** PASS. Correctly identified the unmapped exception, traced the concrete consequence (falls into the `Exception.class` catch-all → 500 instead of 400, contradicting CLAUDE.md's stated exception-handling convention), and correctly recognized what *wasn't* a problem (validation placed in the service rather than as a Bean Validation annotation, consistent with `InvalidDateRangeException`/`InvalidSpendingComparisonRequestException`).
