# Task Evidence: Offline-First Sync Engine

## 1. Outcome

Implemented the offline-first synchronization engine for Pocket Budget in strict adherence to [`.agents/contracts/offline-first-sync-engine.md`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/.agents/contracts/offline-first-sync-engine.md).

Room remains the single local source of truth with 0ms UI latency. When a signed-in user creates, edits, or deletes claimed records, changes are persisted immediately to Room and queued atomically into a persistent Room-backed outbox (`sync_outbox`). Local deletes on claimed records create soft-delete tombstones (`deletedAtEpochMillis`), hiding them from user queries while preserving them for remote propagation. Multiple edits coalesce into a single pending mutation. When online, `PocketSyncEngine` pushes mutations to Supabase via the `apply_sync_mutations` RPC and pulls changes via `pull_sync_changes` using deterministic Last-Write-Wins (LWW) conflict resolution on `(updatedAtEpochMillis, deviceId, mutationId)`. Unclaimed local records (`userId = NULL`) remain strictly device-only and are never uploaded or synced.

---

## 2. Scope & In-Scope Deliverables

### In-Scope Deliverables
1. **Dependencies**:
   - Added `libs.supabase.postgrest` (from existing `supabase-bom = "3.2.2"`) in `gradle/libs.versions.toml` and `shared/build.gradle.kts`.
2. **Room Schema Migration (v5 $\to$ v6)**:
   - Updated `TransactionEntity`, `MonthlyBudgetEntity`, `CategoryBudgetEntity` with sync tracking columns `updatedAtEpochMillis: Long` and `deletedAtEpochMillis: Long? = null`.
   - Created `SyncOutboxEntity` (table `sync_outbox`) and `SyncMetadataEntity` (table `sync_metadata`).
   - Implemented `MIGRATION_5_6` in `DatabaseBuilder.kt` with index creation on `(userId, status)`.
   - Verified with `RoomMigrationTest.kt` preserving pre-existing data and initializing timestamps.
3. **DAOs & Push-Down Soft-Delete Isolation**:
   - `TransactionDao`, `BudgetDao`, `CategoryBudgetDao` updated with `AND deletedAtEpochMillis IS NULL` across all active user queries.
   - Added atomic `@Transaction` methods: `insertWithOutbox`, `updateWithOutbox`, `deleteWithOutbox`, `insertOrUpdateWithOutbox`, `deleteForMonthWithOutbox`.
   - `SyncOutboxDao` and `SyncMetadataDao` created with full mutation lifecycle and durable cursor tracking.
4. **Device Identity Provider**:
   - `DeviceIdProvider` interface with persistent implementations in JVM (`PlatformModule.jvm.kt`), Android (`PlatformModule.android.kt`), and iOS (`PlatformModule.ios.kt`) providing stable installation UUIDs for tie-breaking.
5. **Deterministic Conflict Resolution**:
   - `SyncConflictResolver`: deterministic LWW comparison matching the contract's strict lexicographical ordering:
     1. `updatedAtEpochMillis` (higher wins)
     2. `deviceId` (greater wins)
     3. `mutationId` (greater wins)
   - Tested in `SyncConflictResolverTest.kt`.
6. **Outbox Coalescing & Repositories**:
   - `RoomTransactionRepository`, `RoomBudgetRepository`, `RoomCategoryBudgetRepository`: atomic entity + outbox writes for claimed records; unclaimed records remain local-only with no outbox entries.
   - Outbox coalescing preserves original `mutationId` and `createdAtEpochMillis` for idempotency while updating payload and `updatedAtEpochMillis`.
   - Claiming local data via `RoomDataOwnershipRepository.claimLocalDataAtomically` atomically updates ownership and queues `UPSERT` mutations in `sync_outbox`.
   - Tested in `SyncRepositoryTest.kt`.
7. **Local Supabase Database Protocol & Tests**:
   - Migration `supabase/migrations/20260920150000_create_sync_protocol.sql`:
     - Added `device_id`, `mutation_id`, `updated_at_epoch_millis` to `transactions`, `monthly_budgets`, `category_budgets`.
     - Created `is_mutation_winner(...)` matching Kotlin conflict resolution.
     - Created RPC `apply_sync_mutations(p_mutations JSONB)`: `SECURITY DEFINER`, cross-user rejection, idempotent retry, LWW upsert/soft-delete.
     - Created RPC `pull_sync_changes(p_since_epoch_millis BIGINT)`: returns active records and tombstones, advances cursor.
     - Revoked from `PUBLIC` and `anon`; granted to `authenticated`.
   - Local database tests `supabase/tests/02_sync_protocol_test.sql`: 19 pgTAP tests verifying anon rejection, user mutation, cross-user rejection, idempotent retry, invalid input rejection, LWW conflict resolution, and soft-delete tombstone pull.
8. **Sync Engine & Koin Composition**:
   - `SyncProtocolModels.kt`: DTOs and `RemoteSyncDataSource` interface.
   - `PocketSyncEngine`: handles push, pull, conflict resolution, retry tracking, and durable cursor advancement.
   - `SupabaseRemoteSyncDataSource`: client implementation calling Supabase Postgrest RPCs.
   - `AppModule.kt`: registered `syncModule` providing `RemoteSyncDataSource`, `SyncEngine`, and `SyncCoordinator` in Koin.
   - Tested in `PocketSyncEngineTest.kt` (9/9 unit tests passing).
9. **Application-Level Sync Coordination**:
   - `AppSyncState.kt`: Pure domain state model (`Idle`, `Pending`, `Syncing`, `Success`, `Error`).
   - `SyncCoordinator.kt`: Pure domain service interface (`syncState: StateFlow<AppSyncState>`, `requestSync(): Job`).
   - `PocketSyncCoordinator.kt`: Coordinates sync lifecycle:
     - Only starts sync for authenticated users with valid Supabase configuration (`config.isConfigured`).
     - Triggers initial sync attempt upon user authentication.
     - Reactively observes `syncOutboxDao.observePendingMutationCount(userId)` and processes pending mutations.
     - Prevents overlapping sync runs for the same user via `Mutex`.
     - Returns immediately to `Idle` upon user sign-out or when unauthenticated; preserves local data and pending mutations upon failure with truthful `AppSyncState.Error`.
   - **Repository Outbox Triggers**:
     - `RoomTransactionRepository`, `RoomBudgetRepository`, `RoomCategoryBudgetRepository`, and `RoomDataOwnershipRepository` invoke `syncCoordinator?.requestSync()` whenever claimed mutations are written to `sync_outbox`.
     - Unclaimed operations never trigger sync requests.
     - Screen ViewModels depend only on domain use cases and never call `PocketSyncEngine` or `SyncCoordinator` directly.
   - Tested in `PocketSyncCoordinatorTest.kt` (8/8 unit tests passing), `SyncRepositoryTest.kt` (3 coordinator trigger tests passing), and `LocalSupabaseRuntimeSyncIntegrationTest.kt` (end-to-end runtime integration test passing against local Supabase).

### Explicit Exclusions Honored
- Background scheduling via Android WorkManager, iOS background tasks, or desktop background services is strictly omitted.
- Realtime subscriptions and live multi-device WebSocket listeners are omitted.
- No screen ViewModel calls `PocketSyncEngine` or `SyncCoordinator` directly.
- No hosted Supabase changes executed; hosted project `hyetpnyruynichpsxyvz` remains strictly read-only.
- Money representation strictly preserved as `Long` integer cents everywhere.

---

## 3. Test-First Evidence (RED $\to$ GREEN)

### RED Phase
1. **Room Migration (v5 $\to$ v6)**:
   - In `RoomMigrationTest.kt`, attempted to query `updatedAtEpochMillis` and `sync_outbox` before adding `MIGRATION_5_6`. Failed with missing column/table error.
2. **Conflict Resolution Ordering**:
   - In `SyncConflictResolverTest.kt`, verified comparison logic failed prior to implementing the 3-element tuple comparator.
3. **Sync Repository Outbox Integration**:
   - In `SyncRepositoryTest.kt`, verified that claimed operations produced outbox records and soft-delete tombstones; failed prior to repository atomic write integration.
4. **Local Supabase Database Security & Protocol**:
   - In `supabase/tests/02_sync_protocol_test.sql`, verified anonymous execution failed with `42501` and cross-user tampering was rejected.
5. **PocketSyncEngine Unit Tests**:
   - In `PocketSyncEngineTest.kt`, verified test failures on unacknowledged push and rejected outbox updates prior to tuning outbox querying and pull reconciliation.

### GREEN Phase
1. **Room Migration Test**:
   - `RoomMigrationTest.kt`: PASS (migrations 2->3, 3->4, 4->5, 5->6)
2. **Conflict Resolution Unit Tests**:
   - `SyncConflictResolverTest.kt`: PASS (tuple ordering, deviceId tie-breaking, mutationId tie-breaking)
3. **Repository Outbox & Coalescing Tests**:
   - `SyncRepositoryTest.kt`: PASS (7/7 tests covering unclaimed isolation, claimed insert, update coalescing, soft delete, budget outbox, claiming outbox)
4. **Sync Engine Tests**:
   - `PocketSyncEngineTest.kt`: PASS (9/9 tests covering push success, stale removal, rejected failure update, network retry, pull inserts, conflict remote wins, conflict local wins, soft-delete tombstones, unavailable without remote)
5. **Local Supabase Protocol Tests**:
   - `supabase test db --local`: PASS (44/44 tests across `01_security_and_constraints_test.sql` and `02_sync_protocol_test.sql`)
6. **Sync Coordinator Unit Tests**:
   - `PocketSyncCoordinatorTest.kt`: PASS (8/8 tests verifying auth state validation, mutual exclusion, initial sync on sign-in, reactive outbox sync, sign-out transition to idle, retry preservation on error)
7. **Repository Coordinator Trigger Tests**:
   - `SyncRepositoryTest.kt`: PASS (10/10 tests, including 3 tests verifying claimed transactions invoke `requestSync()` and unclaimed operations do not)
8. **Runtime End-to-End Integration Test**:
   - `LocalSupabaseRuntimeSyncIntegrationTest.kt`: PASS (runtime path: seed unclaimed transaction -> sign up -> claim data -> coordinator syncs record to local Supabase Postgres -> update propagates to remote -> delete soft-deletes in Postgres -> sign out resets coordinator to Idle)

---

## 4. Shared Verification Gate Report

Executed `./scripts/verify.sh`:

```text
==================================================
          Verification Gate Summary
==================================================
  Git diff check (whitespace/formatting) : PASS
  Shared JVM Unit Tests (:shared:jvmTest) : PASS
  Android Host Unit Tests (:shared:testAndroidHostTest) : PASS
  Desktop App Compilation (:desktopApp:compileKotlin) : PASS
  Android App Packaging (:androidApp:assembleDebug) : PASS
  iOS Simulator Tests (:shared:iosSimulatorArm64Test) : NOT RUN (Target requires ARM64, host is macOS x86_64)
==================================================
✔ All active verification checks PASSED.
```

---

## 5. Remote Service & Governance Status

- **Environment Target**: Development project `hyetpnyruynichpsxyvz` (`Pocket Budget Dev`).
- **Hosted Supabase Status**: **Applied & Verified**. The sync protocol migration `create_sync_protocol` (version `20260920170836`) was applied to the hosted development project `hyetpnyruynichpsxyvz` following explicit user approval.
- **Hosted Verification**:
  - `list_migrations`: `create_secure_remote_schema` (`20260917150352`), `create_sync_protocol` (`20260920170836`).
  - Table schemas (`transactions`, `monthly_budgets`, `category_budgets`): RLS enabled, device_id, mutation_id, updated_at_epoch_millis columns present.
  - RPC functions: `apply_sync_mutations`, `pull_sync_changes`, `is_mutation_winner` active with explicit `SET search_path = public`.
- **Local Verification Status**:
  - `supabase db reset --local --yes`: PASS
  - `supabase db lint --local`: PASS (0 lint errors)
  - `supabase test db --local`: PASS (44/44 tests passed)

---

## 6. Truthful Sync Feedback & Responsive Control

### Architecture & Boundaries
1. **Connectivity Boundary (`ConnectivityObserver`)**:
   - Pure domain interface: `val isOnline: StateFlow<Boolean>`.
   - Multiplatform implementations:
     - Android: `AndroidConnectivityObserver` registering `ConnectivityManager.NetworkCallback` with internet capability checks.
     - iOS: `IosConnectivityObserver` utilizing `NWPathMonitor` on main dispatch queue.
     - JVM: `JvmConnectivityObserver` checking active non-loopback network interfaces periodically.
2. **Domain Status Model (`SyncStatus`)**:
   - 7 canonical states:
     - `SignedOut`: Device in local-only unauthenticated mode.
     - `Unconfigured`: Backend unconfigured or missing keys.
     - `Offline(pendingCount)`: Network offline, showing local mode or queued changes.
     - `Pending(pendingCount)`: Network online, local changes waiting to sync.
     - `Syncing(pendingCount)`: Active synchronization in progress.
     - `Success(lastSyncedAtEpochMillis, pushedCount, pulledCount)`: Cloud synced and up to date.
     - `Failure(message, pendingCount, canRetry)`: Sync attempt failed, local changes preserved, retry status exposed.
3. **Use Cases**:
   - `GetSyncStatusUseCase`: Pure domain flow combining auth state, connectivity, coordinator state, outbox pending count, and backend configuration.
   - `RequestSyncUseCase`: Allows presentation layer to request sync without direct coordinator or engine dependencies.
4. **Shell & Responsive Indicators (`PocketNavigationShell`)**:
   - Compact: Compact banner atop screen for active states (`Offline`, `Syncing`, `Pending`, `Failure`) which collapses during steady states (`Success`, `SignedOut`, `Unconfigured`). Clicking navigates to Account screen.
   - Expanded: Sidebar indicator showing color status dot, title, and subtitle. Clicking navigates to Account screen.
5. **Account Screen Sync Controls (`AccountContent`)**:
   - Cloud Synchronization card with network status badge ("Online" / "Offline").
   - Truthful state titles and descriptions with localized relative timestamp formatting.
   - "Sync Now" button (for manual triggering when online and not syncing).
   - "Retry Sync" button (when in failure state with retry allowed).

### Verification & Automated Testing
- **Domain Use Case Tests**: `GetSyncStatusUseCaseTest.kt` (7/7 tests verifying all distinct status permutations).
- **Coordinator Tests**: `PocketSyncCoordinatorTest.kt` (11/11 tests, covering outbox pending count observation, offline error handling without invoking engine, and offline-to-online transition sync).
- **Presentation ViewModel Tests**:
  - `PocketShellViewModelTest.kt` (reactive status collection).
  - `AccountViewModelTest.kt` (auth states, sync states, button triggers).
- **UI Journey & Component Tests**:
  - `PocketNavigationShellUiTest.kt` (compact active banner, compact steady-state collapse, expanded sidebar indicator, and Account navigation clicks).
  - `AccountSyncCardUiTest.kt` (all card states, network badges, descriptions, and "Sync Now" / "Retry Sync" action button clicks).
- **Shared Verification Gate**: All active checks PASSED via `./scripts/verify.sh`.

---

## 7. Multiplatform Supabase Runtime Configuration

### Architecture & Platform Implementations
1. **Explicit Environments (`SupabaseEnvironment`)**:
   - `LOCAL`, `HOSTED_DEV`, and `UNCONFIGURED`.
   - Strict resolution and validation via `SupabaseConfig.resolve(...)`. Enforces URL schemes (`http://`/`https://` for `LOCAL`; strictly `https://` for `HOSTED_DEV`). Blank, invalid, or missing values safely resolve to `UNCONFIGURED` without crashing app startup.
   - Fail-safe auth client initialization: caught initialization errors in `SupabaseAuthRepository` safely evaluate to `client = null`, keeping local Room budgeting 100% operational offline.
   - Clean Koin dependency composition: `RemoteSyncDataSource?` constructed inline within `SyncEngine` bean definition, preventing null-bean `InstanceCreationException` when unconfigured.
2. **Desktop (JVM)**:
   - Reads JVM system properties (`supabase.environment`, `supabase.url`, `supabase.publishableKey`) with fallback to OS environment variables.
   - Automatically forwarded in `desktopApp/build.gradle.kts`.
3. **Android**:
   - Injected into `BuildConfig` via `androidApp/build.gradle.kts` resolving from `-P` properties, `local.properties`, and environment variables.
   - Declared `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` in manifests and guarded `AndroidConnectivityObserver.checkCurrent()` against `SecurityException`.
4. **iOS**:
   - Bundled via `Info.plist` referencing build settings from `iosApp/Config.xcconfig` (with template in `Config.xcconfig.template`). Safely guards against unexpanded `$(VARIABLE)` template strings.
5. **Presentation & Screen Decomposition**:
   - Refactored `SetupConfig` screen quad into an informative **Environment Details** screen displaying the active environment badge, masked credentials, platform configuration instructions, and a return button.
   - Updated `AccountViewModel` to dynamically format environment status.
6. **Documentation**:
   - Comprehensive documentation added to `docs/configuration.md` with placeholders only (`<PROJECT_REF>`, `<ANON_KEY>`, etc.) and linked from `README.md`.

### Verification & Automated Testing
- **Config Unit Tests**: `SupabaseConfigTest.kt` (10/10 tests PASS validating environments, schemes, trimming, and unconfigured fallback).
- **Desktop JVM Config Tests**: `JvmSupabaseConfigTest.kt` (5/5 tests PASS validating JVM properties, env vars, precedence, and malformed inputs).
- **Account ViewModel Tests**: `AccountViewModelTest.kt` (PASS verifying dynamic environment title and sync controls).
- **Auth UI Journey Tests**: `AuthJourneyUiTest.kt` (PASS verifying UI journeys with test configuration double).
- **Live Integration Test Guard**: `LocalSupabaseRuntimeSyncIntegrationTest.kt` cleanly skipped when local Docker stack is unreachable, preventing false build failures.
- **Shared Verification Gate**: All active checks PASSED via `./scripts/verify.sh`.

