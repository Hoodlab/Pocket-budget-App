---
trigger: always_on
---

# Pocket Budget — Project Rules

## Scope

- Build only what the active implementation contract requires.
- Do not add related features, extra fields, APIs, abstractions, or refactors unless they are explicitly requested.

## Money

- Store and calculate monetary values as `Long` integer cents.
- Do not use `Double` or `Float` for money.
- Parse a UI amount directly from text to `Long` cents. Do not use `Double` or `Float` during conversion.

## Date and time

- Use `kotlin.time.Instant` for transaction timestamps.
- Do not introduce another timestamp type without an explicit project decision.

## Architecture boundaries

- Domain code must not depend on Room, SQLite, Compose, Android, iOS, or desktop APIs.
- Repository contracts belong in the domain layer.
- Room entities, DAOs, databases, and mappers belong in the data layer.

## Change discipline

- Follow the `test-first` workflow for new observable domain and data behavior.
- Do not add or change Gradle dependencies without explicit approval.
- Do not commit or push unless explicitly requested.

## Checkpoints

- Use the `verify-and-checkpoint` workflow before creating a commit.
- The agent must wait for explicit approval before staging, committing, or pushing.