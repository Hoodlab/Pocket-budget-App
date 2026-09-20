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
   - `AppModule.kt`: registered `syncModule` providing `RemoteSyncDataSource` and `SyncEngine` in Koin.
   - Tested in `PocketSyncEngineTest.kt` (9/9 unit tests passing).

### Explicit Exclusions Honored
- Background scheduling via Android WorkManager, iOS background tasks, or desktop background services is strictly omitted.
- Realtime subscriptions and live multi-device WebSocket listeners are omitted.
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
- **Hosted Supabase Status**: **Read-Only**. Zero mutations or migrations applied to hosted Supabase during this slice.
- **Local Verification Status**:
  - `supabase db reset --local --yes`: PASS
  - `supabase db lint --local`: PASS (0 lint errors)
  - `supabase test db --local`: PASS (44/44 tests passed)
- **Pending Hosted Mutation**: `supabase/migrations/20260920150000_create_sync_protocol.sql` is ready for review. In accordance with project rules and the `remote-service-change` workflow, it will only be deployed after explicit human approval immediately prior to execution.
