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

- Run the relevant verification for the changed files.
- Use the exact project commands when they are known.
- Do not invent test tasks.
- Report each check as PASS, FAIL, or NOT RUN.

## Report before changing Git state

Report:

- files changed
- whether the work stays inside the active contract
- verification results
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