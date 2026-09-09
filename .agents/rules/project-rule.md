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
- Shared feature UI belongs in `presentation/<feature>/`; each independently navigable screen gets its own subpackage (`presentation/<feature>/<screen>/`), keeping the outer feature package as the boundary; shared theme belongs in `presentation/theme/`; typed routes and the navigation host belong in `navigation/`.
- Platform source sets contain platform glue only. Business and presentation logic stay in `commonMain`.
- Each screen has a `<Feature>ViewModel`, `<Feature>UiState`, `<Feature>Screen`, and `<Feature>Content`.
- `<Feature>Screen` collects state, handles one-time events, and connects callbacks. It contains no layout.
- `<Feature>Content` receives a complete `UiState` plus event lambdas. It has no ViewModel, repository, use-case, or navigation dependency.
- ViewModels depend on domain use cases only. They must not depend on Room, repositories, or navigation controllers.
- ViewModels expose one-time outcomes as events. They do not accept navigation callbacks.
- Routes use serializable typed objects. Routes carry identifiers, never full domain objects. The navigation host owns back-stack behaviour.
- UI code uses theme tokens or `MaterialTheme`; do not hardcode colours.
- Add a realistic preview for each stateless screen content composable when supported by the configured targets. Record a real exception when it is not supported.

## Change discipline

- Follow the `test-first` workflow for new observable domain and data behavior.
- Do not add or change Gradle dependencies without explicit approval.
- Do not commit or push unless explicitly requested.

## UI quality gate

- Automated UI tests must cover key user journeys and edge cases (add, invalid input, edit, cancel/confirm deletion, cross-screen navigation).
- UI tests use stable semantic test tags only where needed, avoiding brittle layout dependencies.
- Visual-validation evidence must be captured and visually reviewed against reference designs in `docs/design/reference/` before completing a UI slice. Automated screenshots write to build outputs first, with only approved images added to durable evidence.
- Record every verification command that runs; mark unsupported targets as NOT RUN with the real reason.

## Checkpoints

- Use the `verify-and-checkpoint` workflow before creating a commit.
- The agent must wait for explicit approval before staging, committing, or pushing.