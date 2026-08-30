---
description: Use for a small observable behavior change in Pocket Budget that needs a genuine RED → GREEN test cycle.
---

# Test-First Change

Use this workflow for one small observable behavior change.

## Define the behavior

Restate the requested behavior in one sentence.

State what is out of scope. Do not add adjacent features, dependencies, abstractions, refactors, or unrelated fixes unless explicitly requested.

## Inspect before changing

Read the directly relevant production code and tests.

Identify:

- the narrowest appropriate test location
- the focused test command
- the wider verification required after GREEN

Do not modify production code during this step.

## RED

Add or change one focused test for the requested behavior.

Run the focused test before changing production code.

The test must fail because the requested behavior is missing.

If the test passes immediately, stop and report why. Do not claim RED evidence.

A compilation error, missing dependency, typo, incorrect task, or test-configuration failure is not valid RED evidence.

## GREEN

Make the smallest production-code change that makes the same focused test pass.

Do not broaden the scope.

Run the same focused test again and report the exact GREEN result.

## Wider verification

Run:

./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest

Report each result as PASS, FAIL, or NOT RUN.

## Final report

Report:

- behavior changed
- files changed
- RED command and truthful result
- GREEN command and truthful result
- wider verification results
- limitations or unverified targets

Do not commit unless explicitly requested.

Never claim a RED → GREEN cycle if the test was written after the implementation already worked.