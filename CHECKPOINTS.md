# Pocket Budget — Student Checkpoints Guide

Welcome to the **Pocket Budget** learning guide! This repository is organized to give you both a clean starter slate to build from scratch and an incremental set of checkpoint tags to inspect, compare, and verify your implementation at each step.

---

## 1. Branch and Repository Structure

| Branch / Tag | Description | Purpose |
|---|---|---|
| **`dev`** | Initial starter project (`checkpoint-00-starter`) | **Your working branch.** Checkout this branch to start building your own implementation from scratch. |
| **`main`** | Complete, production-grade reference implementation | The final source of truth. Contains all features, tests, migrations, and documentation. |
| **`checkpoint-XX-*`** | Semantic git tags marking major milestones | Fixed reference points across the curriculum. Use them to inspect code or compare with your own work. |

---

## 2. Checkpoint Curriculum Overview

Each checkpoint represents a cohesive, test-verified feature slice.

```
[dev / checkpoint-00-starter]
        │
        ▼
[checkpoint-01-transaction-basics] ──► Domain Transaction model, Room SQLite persistence, Add & List UI
        │
        ▼
[checkpoint-02-transaction-browsing] ──► Typed Compose navigation, month browsing, dual-viewport UI
        │
        ▼
[checkpoint-03-monthly-budget] ──────► Monthly budget CRUD, SQLite expense aggregation, edit & delete flows
        │
        ▼
[checkpoint-04-category-budgets] ───► Predefined categories, category budget limits, spending & progress
        │
        ▼
[checkpoint-05-dashboard] ──────────► Cash flow overview, read-only budget health indicators
        │
        ▼
[checkpoint-06-auth-and-claiming] ──► Supabase Auth, local data claiming, responsive PocketNavigationShell
        │
        ▼
[checkpoint-07-koin-di] ────────────► Koin dependency injection composition across shared and platform roots
        │
        ▼
[checkpoint-08-sync-engine] ────────► Offline-first sync engine, persistent outbox, RLS sync protocol
        │
        ▼
[checkpoint-09-sync-coordination] ──► SyncCoordinator, network connectivity observation, UI status feedback
        │
        ▼
[checkpoint-10-runtime-config] ─────► Multiplatform runtime configuration (Desktop JVM, Android BuildConfig, iOS xcconfig)
        │
        ▼
[main / Final Project] ─────────────► Fully tested, verified, multiplatform personal budgeting application
```

---

## 3. Detailed Checkpoint Breakdown

### Checkpoint 00: Starter Baseline
- **Tag:** `checkpoint-00-starter` (also branch `dev` and tag `lesson-01-start`)
- **Focus:** Clean multiplatform starter template.
- **Key Concepts:**
  - Standard Kotlin Multiplatform & Compose Multiplatform skeleton.
  - Three target platforms: Android (`androidApp`), Desktop JVM (`desktopApp`), and iOS (`iosApp`).
  - Shared UI and logic in `shared/src/commonMain`.
- **Run & Test:**
  ```bash
  ./gradlew :desktopApp:run
  ./gradlew :shared:jvmTest
  ```

---

### Checkpoint 01: Transaction Basics & Persistence
- **Tag:** `checkpoint-01-transaction-basics`
- **Focus:** Core domain model, Room Multiplatform database, and transaction entry/browsing.
- **Key Concepts:**
  - **Financial Precision Rule:** Money represented strictly as `Long` integer cents (e.g., \$12.50 = `1250L`). Never `Double` or `Float`.
  - **Timestamps:** `kotlin.time.Instant` for transaction dates.
  - **Clean Architecture:** Pure domain `Transaction` model and `TransactionRepository` interface.
  - **Data Layer:** `TransactionEntity`, Room DAO, SQLite migration, and `RoomTransactionRepository`.
  - **Screen Quad Pattern:** `TransactionListViewModel`, `TransactionListUiState`, `TransactionListScreen`, `TransactionListContent`.
- **Key Files:**
  - `shared/src/commonMain/kotlin/org/hoods/pocket/domain/model/Transaction.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/data/local/TransactionDao.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/transaction/list/`

---

### Checkpoint 02: Typed Navigation & Month Browsing
- **Tag:** `checkpoint-02-transaction-browsing`
- **Focus:** Type-safe Compose navigation, Room push-down date filtering, and dual-viewport layout.
- **Key Concepts:**
  - Type-safe navigation routes using `@Serializable` objects (`Screen.kt`).
  - Date filtering pushed down to SQLite queries (avoiding unbounded in-memory collection filtering).
  - Responsive layout: Adapting between `Compact` (mobile) and `Expanded` (desktop) viewports.
  - Headless UI-flow journey tests and visual validation.
- **Key Files:**
  - `shared/src/commonMain/kotlin/org/hoods/pocket/navigation/Screen.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/navigation/PocketNavHost.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/transaction/TransactionMonthBrowser.kt`

---

### Checkpoint 03: Monthly Budget Management
- **Tag:** `checkpoint-03-monthly-budget`
- **Focus:** Monthly budget limits, expense aggregation, and budget modification/deletion.
- **Key Concepts:**
  - Domain `MonthlyBudget` model with monthly spending targets.
  - Database-level expense aggregation query linking transactions to the active budget period.
  - Confirmation dialogs for destructive actions (deleting budgets).
  - Empty state transitions and form validation.
- **Key Files:**
  - `shared/src/commonMain/kotlin/org/hoods/pocket/domain/model/MonthlyBudget.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/data/local/BudgetDao.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/budget/`

---

### Checkpoint 04: Category Budgets & Allocations
- **Tag:** `checkpoint-04-category-budgets`
- **Focus:** Predefined categories and granular monthly category budget allocations.
- **Key Concepts:**
  - Standard transaction categories (Food, Housing, Transportation, Utilities, Entertainment, Healthcare, etc.).
  - Category-specific spending limits and progress calculation.
  - Visual ratio indicators (`Float` allowed strictly for UI progress ratios, never for financial amounts).
- **Key Files:**
  - `shared/src/commonMain/kotlin/org/hoods/pocket/domain/model/TransactionCategory.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/domain/model/CategoryBudget.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/budget/category/`

---

### Checkpoint 05: Financial Dashboard
- **Tag:** `checkpoint-05-dashboard`
- **Focus:** High-level monthly cash flow overview and read-only budget health summary.
- **Key Concepts:**
  - Aggregated cash flow overview (total income, total expenses, net balance).
  - Visual budget progress bars with health warnings (Normal, Warning, Over Budget).
  - Responsive arrangement across Compact phone cards and Expanded desktop grids.
- **Key Files:**
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/dashboard/DashboardViewModel.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/dashboard/DashboardContent.kt`

---

### Checkpoint 06: Authentication & Local Data Claiming
- **Tag:** `checkpoint-06-auth-and-claiming`
- **Focus:** User authentication with Supabase Auth, account isolation, and claiming local guest data.
- **Key Concepts:**
  - Unauthenticated guest mode: Users can create transactions and budgets before creating an account.
  - Data claiming flow: Upon sign-in, detect unclaimed guest records and atomically assign them to the signed-in user ID across all tables.
  - Push-down `userId` filtering: Queries strictly isolate data per active user.
  - Unified `PocketNavigationShell`: Top-level application shell providing bottom navigation on Compact and sidebar on Expanded screens.
- **Key Files:**
  - `shared/src/commonMain/kotlin/org/hoods/pocket/domain/usecase/ClaimLocalDataUseCase.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/shell/PocketNavigationShell.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/auth/`

---

### Checkpoint 07: Clean Composition Root with Koin DI
- **Tag:** `checkpoint-07-koin-di`
- **Focus:** Decoupling application composition using Koin Multiplatform.
- **Key Concepts:**
  - Shared Koin modules (`appModule`) providing repositories, use cases, and ViewModels.
  - Platform Koin modules (`platformModule`) providing platform-specific database drivers and Ktor engines.
  - ViewModels depend strictly on domain use cases (no direct Room or Supabase dependencies).
- **Key Files:**
  - `shared/src/commonMain/kotlin/org/hoods/pocket/di/AppModule.kt`
  - `shared/src/androidMain/kotlin/org/hoods/pocket/di/PlatformModule.android.kt`
  - `shared/src/jvmMain/kotlin/org/hoods/pocket/di/PlatformModule.jvm.kt`
  - `shared/src/iosMain/kotlin/org/hoods/pocket/di/PlatformModule.ios.kt`

---

### Checkpoint 08: Offline-First Sync Engine
- **Tag:** `checkpoint-08-sync-engine`
- **Focus:** Robust offline-first synchronization with persistent outbox and deterministic conflict resolution.
- **Key Concepts:**
  - Local-first architecture: All user operations commit locally to Room first.
  - Persistent sync outbox: Tracks pending upserts and tombstones for deletes.
  - Conflict resolution: Deterministic last-write-wins based on `updatedAtEpochMillis` and `deviceId`.
  - Supabase PostgreSQL sync protocol with Row Level Security (RLS) policies.
- **Key Files:**
  - `shared/src/commonMain/kotlin/org/hoods/pocket/data/sync/PocketSyncEngine.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/domain/service/SyncConflictResolver.kt`
  - `supabase/migrations/20260920150000_create_sync_protocol.sql`

---

### Checkpoint 09: Sync Coordination & UI Feedback
- **Tag:** `checkpoint-09-sync-coordination`
- **Focus:** Background synchronization coordination, active network observation, and truthful user feedback.
- **Key Concepts:**
  - `SyncCoordinator`: Reacts to network changes and user sync requests.
  - 7 distinct sync states: `Unconfigured`, `SignedOutLocalOnly`, `IdleSynced`, `PendingSync`, `Syncing`, `Offline`, `Error`.
  - Non-intrusive status indicators in `PocketNavigationShell` and full sync card in Account settings.
- **Key Files:**
  - `shared/src/commonMain/kotlin/org/hoods/pocket/domain/service/SyncCoordinator.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/domain/model/SyncStatus.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/account/AccountSyncCard.kt`

---

### Checkpoint 10: Multiplatform Runtime Configuration
- **Tag:** `checkpoint-10-runtime-config`
- **Focus:** Production runtime configuration across Desktop, Android, and iOS without hard-coded secrets.
- **Key Concepts:**
  - Three explicit environments: `LOCAL`, `HOSTED_DEV`, and `UNCONFIGURED`.
  - Platform-idiomatic configuration injection:
    - **Desktop JVM:** System properties (`-Dpocket.supabase.url=...`) or environment variables.
    - **Android:** `BuildConfig` values configured via Gradle / local properties.
    - **iOS:** `Config.xcconfig` / `Info.plist` build settings.
  - Graceful fallback: Missing or invalid configurations safely resolve to `UNCONFIGURED` without crashing app startup.
- **Key Files:**
  - `shared/src/commonMain/kotlin/org/hoods/pocket/config/SupabaseRuntimeConfig.kt`
  - `docs/configuration.md`

---

## 4. How to Navigate and Compare (Git Guide)

### Starting Your Development
To begin working on your own implementation from the starter code:
```bash
# Switch to the dev starter branch
git checkout dev

# Verify that you are at the initial commit
git log -n 1 --oneline
# Expected output: 978e055 Initial Pocket Budget starter project
```

---

### Inspecting a Checkpoint
You can check out any checkpoint tag at any time to run it, view the code, or test its behavior:
```bash
# Checkout Checkpoint 3 in "detached HEAD" mode
git checkout checkpoint-03-monthly-budget

# Run the desktop app at this checkpoint
./gradlew :desktopApp:run

# When finished inspecting, return safely to your dev branch:
git checkout dev
```
> [!NOTE]
> Checking out a tag puts git in "detached HEAD" mode. This is completely safe for reading, testing, and running code. When you want to continue your own work, simply run `git checkout dev`.

---

### Comparing Your Code with a Checkpoint
Want to check how your implementation compares to a specific checkpoint? Use `git diff`:

```bash
# 1. See all differences between your current code and Checkpoint 1:
git diff checkpoint-01-transaction-basics

# 2. See only the list of files that differ:
git diff --stat checkpoint-01-transaction-basics

# 3. Compare a specific file against a checkpoint:
git diff checkpoint-01-transaction-basics -- shared/src/commonMain/kotlin/org/hoods/pocket/domain/model/Transaction.kt

# 4. Compare your entire dev branch against the final completed reference:
git diff dev..main --stat
```

---

### Comparing Two Checkpoints
To understand what changed between two consecutive checkpoints:

```bash
# Summary of files changed between Checkpoint 1 and Checkpoint 2:
git diff --stat checkpoint-01-transaction-basics..checkpoint-02-transaction-browsing

# Detailed line-by-line diff between Checkpoint 1 and Checkpoint 2:
git diff checkpoint-01-transaction-basics..checkpoint-02-transaction-browsing

# View commits introduced between two checkpoints:
git log --oneline checkpoint-01-transaction-basics..checkpoint-02-transaction-browsing
```

---

### Viewing a Reference File Without Switching Branches
You do not need to change branches to look at a reference file from `main` or any checkpoint:

```bash
# View a file from main while you remain on dev:
git show main:shared/src/commonMain/kotlin/org/hoods/pocket/domain/model/Transaction.kt

# View a file from a specific checkpoint:
git show checkpoint-06-auth-and-claiming:shared/src/commonMain/kotlin/org/hoods/pocket/domain/usecase/ClaimLocalDataUseCase.kt
```

---

## 5. Architectural Invariants to Maintain

As you build Pocket Budget, adhere to the project's core architectural rules:

1. **Monetary Precision:** Always use `Long` integer cents (`1000L` for \$10.00). Never use `Double` or `Float` for money.
2. **Timestamps:** Use `kotlin.time.Instant` for transaction and audit timestamps.
3. **Screen Quad Pattern:** Every feature screen follows:
   - `<Feature>ViewModel`: Manages state and emits one-time channel events.
   - `<Feature>UiState`: Immutable data representation of screen state.
   - `<Feature>Screen`: Collects state, handles navigation callbacks, contains no layout.
   - `<Feature>Content`: Stateless composable receiving state and event lambdas.
4. **Clean Architecture Boundaries:**
   - Domain layer has **zero** dependencies on Room, SQLite, Compose, Supabase, or platform APIs.
   - Room entities and DAOs stay in the `data` layer.
   - ViewModels depend **only** on domain use cases.
5. **Push-Down Filtering:** Filter by `userId` and date ranges at the database level (`DAO`), never by fetching unbounded lists into memory.
6. **Offline-First:** Local Room persistence is the primary source of truth; remote sync operates asynchronously in the background.

Happy coding! If you get stuck at any step, use `git diff` against the corresponding checkpoint tag to see the exact changes and structure.
