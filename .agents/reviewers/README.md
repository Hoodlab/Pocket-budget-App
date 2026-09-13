# Pocket Budget — Focused Reviewer Agents

This directory documents the four focused, read-only reviewer agents configured for Pocket Budget:

1. **[Scope Reviewer](scope.md)** (`scope-reviewer`): Contract alignment, exclusion enforcement, abstraction control, and working tree hygiene.
2. **[Architecture Reviewer](architecture.md)** (`architecture-reviewer`): Clean Architecture boundaries, KMP source-set isolation, screen quad decomposition, and unidirectional data flow.
3. **[Domain Reviewer](domain.md)** (`domain-reviewer`): Financial precision (`Long` integer cents), visual ratio `Float` rules, timestamp uniformity (`kotlin.time.Instant`), entity invariants, and push-down query filtering.
4. **[TDD & Evidence Reviewer](tdd-evidence.md)** (`tdd-evidence-reviewer`): Authentic RED $\to$ GREEN test cycles, contract-scoped UI journeys, dual-viewport visual matrices, and truthful verification gate reporting.

---

## Core Operational Principles

### 1. Read-Only with Respect to Project Changes
- Reviewers **must not** modify source code, edit documentation, stage files (`git add`), create commits (`git commit`), push (`git push`), or mutate external databases/services.
- Reviewers have shell access (`run_command`) for **read-only inspection**: inspecting Git status (`git status --short`), diffs (`git diff`), commit logs (`git log`), searching code (`grep_search`, `find_by_name`), and inspecting files (`view_file`).

### 2. Coordinator Verification Gate & Targeted Checks
- The **main coordinator agent** runs the full multiplatform verification gate once via `./scripts/verify.sh`.
- Reviewers evaluate the shared verification gate output. They **must not** automatically rerun the full suite.
- If a reviewer needs to verify a specific concern, it may run a single focused check (e.g. `./gradlew :shared:jvmTest --tests "..."`).
- Reviewers **must not run Gradle tasks concurrently** to prevent Gradle daemon locking and heap exhaustion.
- Any reviewer Gradle command must follow the project's sandbox-bypass setting (`unsandboxed(./gradlew)` / `sandboxMode: false`).

### 3. Diff-Scoped Assessment & Known Debt
- Reviewers evaluate the **active git diff and directly affected paths**.
- Pre-existing issues or test gaps outside the active contract/diff must be recorded under **Known Debt (Non-blocking)** and must **not** block the current feature.
- A `REJECT` verdict is issued only for regressions, contract violations, or evidence gaps introduced by the current change.

### 4. Binary Verdict & Clear Action
- Each reviewer returns either **`APPROVE`** or **`REJECT`**.
- Findings are mapped directly to the reviewer's checklist items.
- If `REJECT`, specific and actionable steps to resolve the issue must be provided.

---

## Antigravity Durable Location & Invocation

- Durable Antigravity skills are defined under `.agents/skills/<reviewer-name>/SKILL.md`.
- Runtime subagents can be invoked concurrently via `invoke_subagent` using their respective registered names (`scope_reviewer`, `architecture_reviewer`, `domain_reviewer`, `tdd_evidence_reviewer`).
