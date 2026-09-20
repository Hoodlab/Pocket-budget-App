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
- When a user-controlled filter applies to persisted data, the repository and database query must receive that filter. Do not load an unbounded stored collection and filter it in a ViewModel unless the contract explicitly justifies it.
- Shared feature UI belongs in `presentation/<feature>/`; each independently navigable screen gets its own subpackage (`presentation/<feature>/<screen>/`), keeping the outer feature package as the boundary; shared theme belongs in `presentation/theme/`; typed routes and the navigation host belong in `navigation/`.
- Platform source sets contain platform glue only. Business and presentation logic stay in `commonMain`.
- Each screen has a `<Feature>ViewModel`, `<Feature>UiState`, `<Feature>Screen`, and `<Feature>Content`.
- `<Feature>Screen` collects state, handles one-time events, and connects callbacks. It contains no layout.
- `<Feature>Content` receives a complete `UiState` plus event lambdas. It has no ViewModel, repository, use-case, or navigation dependency.
- ViewModels depend on domain use cases only. They must not depend on Room, repositories, or navigation controllers.
- ViewModels expose one-time outcomes as events. They do not accept navigation callbacks.
- Routes use serializable typed objects. Routes carry identifiers, never full domain objects. The navigation host owns back-stack behaviour.
- Top-level navigation chrome (such as bottom navigation bars and responsive sidebars) belongs in the shared application shell (`PocketNavigationShell`) wrapping the navigation host. Feature screens must not construct their own top-level sidebars or bottom navigation chrome.
- Authentication and data-ownership decisions (such as determining whether unassigned local data requires claiming upon sign-in) are owned by the relevant ViewModel (`SignInViewModel`) emitting explicit outcomes (`ClaimRequired`), while navigation hosts only respond to ViewModel events.
- UI code uses theme tokens or `MaterialTheme`; do not hardcode colours.
- Add a realistic preview for each stateless screen content composable when supported by the configured targets. Record a real exception when it is not supported.

## Responsive layout

- Layout must respond to available window size, not the device platform.
- Derive a shared `AppLayoutMode` from Material window size classes at the application shell.
- Use `Compact`, `Medium`, and `Expanded` modes rather than per-screen `isDesktop` checks or repeated hard-coded breakpoints.
- The shared application shell (`PocketNavigationShell`) around `PocketNavHost` owns global responsive navigation decisions, keeping top-level destinations (Transactions, Budgets, Account) accessible across compact bottom navigation and wider sidebars.
- Screen content receives the shared layout mode and adapts its own arrangement from it.
- Feature screens and content must never render their own top-level application navigation chrome.
- `BoxWithConstraints` is allowed only for a component’s local measurement needs, not for deciding the overall screen layout.
- Responsive UI work must be verified and visually reviewed at compact and expanded viewport sizes when both are supported.

## Dependency injection and application composition

- Dependency construction belongs in Koin modules and platform composition code, not in feature screens, navigation hosts, or stateless content composables.
- Shared Koin modules provide repositories, use cases, and ViewModels.
- Platform Koin modules provide platform-only objects such as Room builders, SQLite drivers, Ktor engines, and runtime configuration.
- Platform entry points start Koin; PocketBudgetRoot starts the UI and must not become a manual dependency construction chain.
- Stateful screens may obtain their ViewModel through Koin. Stateless content composables must receive plain state and event lambdas only.
- ViewModels depend on domain use cases only. They must not depend on Koin APIs, Room, Supabase, navigation controllers, or platform APIs.
- Unit tests should construct ViewModels directly with fakes where practical.

## Change discipline

- Follow the `test-first` workflow for new observable domain and data behavior.
- Do not add or change Gradle dependencies without explicit approval.
- Do not commit or push unless explicitly requested.

## Remote services and Supabase

- For Supabase authentication, remote persistence, Row Level Security, or sync work, follow the `remote-service-change` workflow and use the `supabase-offline-sync` skill.
- Develop and test Supabase schema migrations and RLS policies locally before proposing any hosted Supabase change.
- A hosted Supabase project is read-only until the user explicitly approves the exact remote mutation immediately before it is made.
- Remote schema, RLS, authentication, and policy changes must be source controlled and reproducible from repository migration files. Do not rely on dashboard-only changes.
- Room remains the source of truth for the UI. Screens read local Room data; remote data synchronises into Room in the background.
- Every user-owned remote row requires authenticated ownership and Row Level Security enforcing `auth.uid() = user_id`.
- Never place service-role keys, private credentials, or tokens in client code, tests, evidence, or repository files.
- Runtime clients may use only the public Supabase URL and anon/publishable key through approved runtime configuration.
- When sync is introduced, local writes must succeed before network work begins. Failed remote mutations must remain in persistent local sync state for retry.
- Synchronised deletes require tombstones until remote confirmation.
- Conflict behaviour and idempotency rules must be defined in the active implementation contract and tested before implementation.
- Local data created before sign-in must never be silently assigned to an account or uploaded.

## UI quality gate

- Automated UI tests must cover key user journeys and edge cases (add, invalid input, edit, cancel/confirm deletion, cross-screen navigation).
- UI tests use stable semantic test tags only where needed, avoiding brittle layout dependencies.
- Visual-validation evidence must be captured and visually reviewed against reference designs in `docs/design/reference/` before completing a UI slice. Automated screenshots write to build outputs first, with only approved images added to durable evidence.
- When both mobile and desktop references exist, inspect both before implementation and capture, review, and record evidence for both supported viewport sizes. If one cannot run, record it as NOT RUN with the real reason.
- Record every verification command that runs; mark unsupported targets as NOT RUN with the real reason.

## Checkpoints

- Use the `verify-and-checkpoint` workflow before creating a commit.
- The agent must wait for explicit approval before staging, committing, or pushing.
