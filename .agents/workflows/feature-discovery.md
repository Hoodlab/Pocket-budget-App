---
description: Use before planning or implementing a feature to inspect the relevant project context and report the smallest safe path without modifying files.
---

# Feature Discovery

Use this workflow before planning or implementing a feature.

## Read the contract

Read the feature implementation contract supplied by the developer.

Restate:

- user outcome
- in-scope behavior
- explicit exclusions
- required verification

## Inspect without changing

Inspect only the project areas relevant to the contract:

- module and source-set structure
- existing domain, data, and presentation conventions
- Gradle dependencies and configured test tasks
- directly related tests
- current Git status

Do not modify files, dependencies, Git state, or external services.

## Report facts before recommendations

Report:

1. Relevant modules, packages, and source sets
2. Existing patterns or dependencies that should be reused
3. Relevant tests and actual verification tasks
4. The smallest safe implementation sequence
5. Constraints from existing project rules or contracts
6. Decisions, risks, or blockers requiring developer approval

Separate verified facts from recommendations.

Do not write an implementation plan that adds behavior outside the contract.