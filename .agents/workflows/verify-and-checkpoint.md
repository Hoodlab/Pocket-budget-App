---
description: Use before every commit to inspect scope, run relevant verification, prepare a report, and wait for developer approval.
---

# Verify and Checkpoint

Use this workflow before creating a commit.

## Inspect

- Read the active implementation contract when one exists.
- Run `git status --short`.
- Inspect the relevant diff.
- Run `git diff --check`.

## Verify

- Run the project verification gate:
  ```bash
  ./scripts/verify.sh
  ```
- The verification gate executes:
  - Working tree whitespace and formatting hygiene (`git diff --check`)
  - Common / JVM unit tests (`./gradlew :shared:jvmTest`)
  - Android host unit tests (`./gradlew :shared:testAndroidHostTest`)
  - Desktop application compilation (`./gradlew :desktopApp:compileKotlin`)
  - Android application debug packaging (`./gradlew :androidApp:assembleDebug`)
  - Truthful reporting of the iOS simulator limitation (`:shared:iosSimulatorArm64Test` is `NOT RUN` due to x86_64 host / ARM64 target architecture mismatch)
- If specific targeted tests were run during a test-first cycle, also include their RED -> GREEN results.
- Report each check as PASS, FAIL, or NOT RUN based on the verification gate summary.

## Reviewer Agent Evaluations

Invoke the four focused reviewer agents concurrently (`invoke_subagent`):
- **Scope Reviewer** (`scope_reviewer`)
- **Architecture Reviewer** (`architecture_reviewer`)
- **Domain Reviewer** (`domain_reviewer`)
- **TDD & Evidence Reviewer** (`tdd_evidence_reviewer`)

Reviewers inspect the active contract, project rules, `git diff`, changed files, and the shared verification gate output. They do not edit files or rerun the full test suite.
Ensure all four reviewers return **`APPROVE`** before requesting developer approval. If any reviewer returns `REJECT`, resolve the required actions first.

## Report before changing Git state

Report:

- files changed
- whether the work stays inside the active contract
- verification results (from the verification gate)
- reviewer verdicts (`APPROVE` / `REJECT` from Scope, Architecture, Domain, TDD/Evidence)
- any noted Known Debt (non-blocking)
- limitations or unverified targets
- files that belong in the checkpoint
- a proposed commit message

Do not stage, commit, push, or change Git configuration at this stage.

## Wait for approval

Only after explicit developer approval:

- stage only the approved files
- create the approved commit
- do not push
- run `git status --short`
- report the commit hash and final Git status