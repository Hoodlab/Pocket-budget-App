# Implementation Contract: Offline-First Sync Engine

## 1. User Outcome

- A signed-in user can create, update, or delete transactions and budgets while offline.
- The app updates immediately from local Room data without waiting for the network.
- Each eligible local change is recorded durably for later synchronisation.
- When connectivity and valid account configuration are available, pending changes synchronise safely with the user’s Supabase account.
- A failed synchronisation never removes the local change or silently loses pending work.
- Retrying a mutation does not create duplicate remote records.
- Data belonging to one account is never synchronised, displayed, or reconciled for another account.
- Unclaimed device-only data remains local and is never uploaded.

## 2. In-Scope Behavior

- **Room as Source of Truth:** All screens continue to read from Room. Remote requests never block a local create, update, delete, or visible UI refresh.
- **Eligible Records:** Synchronise only claimed records belonging to the currently authenticated user:
  - transactions;
  - monthly budgets;
  - category budgets.
- **Persistent Sync Outbox:** Add a Room-backed outbox that survives app restart and records pending mutations.
- **Atomic Local Writes:** A claimed record change and its outbox mutation are written in the same Room transaction.
- **Mutation Identity:** Every mutation has a stable, client-generated mutation ID. A retry reuses that same mutation ID.
- **Device Identity:** Each installation has a persistent, non-secret device ID used only for deterministic mutation ordering and conflict resolution.
- **Local Deletion:** Deleting a claimed record creates a local soft-delete tombstone and queues a delete mutation. The tombstone remains until the remote side confirms the mutation according to the sync protocol.
- **Unclaimed Local Data:** Unclaimed records remain local-only. They create no cloud outbox mutation and are never included in push or pull operations.
- **Push:** Pending mutations for the active user are sent to Supabase through a server-enforced mutation protocol.
- **Pull:** Remote changes for the active user, including tombstones, are pulled into Room using a durable per-user sync cursor.
- **Conflict Resolution:** Use deterministic last-write-wins ordering based on this lexicographical tuple:

  ```text
  updatedAtEpochMillis
  → deviceId
  → mutationId
  ```

  The same ordering must be applied locally and remotely.

- **Remote Mutation Result:** The remote protocol returns a clear result for each mutation, such as applied, stale, or rejected. A rejected mutation remains recoverable locally and surfaces an honest sync error state.
- **Sync State:** Expose sync state and last successful sync information to the application layer. User-facing sync status UI is deferred to the next verification journey unless needed for an existing screen to report a real error.

## 3. Explicit Exclusions

- Automatic background scheduling through Android WorkManager, iOS background tasks, or desktop background services.
- Realtime subscriptions or live multi-device updates.
- Social login, password recovery, or Auth-provider changes.
- Bulk account migration, account switching, or merging data from multiple accounts.
- Recurring transactions, attachments, receipts, or other new product features.
- Hosted Supabase changes before local migration and test evidence are complete and explicit human approval is given.
- Any fake “syncing” or “synced” UI state before a real sync result exists.

## 4. Acceptance Criteria

- [ ] A local create, update, or delete for a claimed record updates Room immediately without requiring network access.
- [ ] The same local operation creates or updates one durable pending outbox mutation atomically.
- [ ] Multiple local edits to the same pending record are coalesced according to the defined mutation policy without losing the latest record state.
- [ ] A claimed-record delete creates a soft-delete tombstone and a pending delete mutation.
- [ ] A successful remote mutation is marked complete only after the defined remote success response.
- [ ] A network or server failure leaves the affected outbox mutation available for retry.
- [ ] Retrying the same mutation ID does not duplicate a remote transaction, monthly budget, or category budget.
- [ ] Push and pull operations are always scoped to the authenticated user ID.
- [ ] Unclaimed `userId = NULL` records are never queued, pushed, pulled as account records, or assigned silently.
- [ ] Remote pulls reconcile active rows and tombstones using the defined last-write-wins tuple.
- [ ] A remote record that wins a conflict replaces the local state and is marked synchronised.
- [ ] A local record that wins a conflict remains pending for a later push.
- [ ] Sync metadata, including the per-user cursor, advances only after the corresponding pull is processed safely.
- [ ] Signing out prevents further sync work for that account and does not expose its records to another account.
- [ ] Missing Supabase configuration leaves local Room functionality usable and reports sync as unavailable rather than failing startup.

## 5. Verification Plan

### Test-First Evidence

Provide genuine RED and GREEN evidence for new observable behaviour:

- atomic local write plus outbox creation;
- pending-mutation retry after a simulated remote failure;
- idempotent retry using the same mutation ID;
- soft delete and tombstone retention;
- user-scoped outbox selection;
- last-write-wins reconciliation;
- cursor advancement only after successful processing.

### Shared Application Tests

- Unit tests for outbox coalescing and mutation identity.
- Repository tests using a controlled fake remote data source.
- Sync-engine tests covering push success, push failure, retry, pull, tombstones, and conflict outcomes.
- Tests confirming unclaimed local data creates no sync work.
- Tests confirming Account A’s records and cursor are never used for Account B.
- Existing transaction, budget, category, account, claim-data, and sign-out journeys remain green.

### Local Supabase Tests

Before any hosted change:

```bash
supabase db reset --local --yes
supabase db lint --local
supabase test db --local
```

Local database tests must prove:

- unauthenticated mutation attempts are rejected;
- one user cannot mutate another user’s records;
- server mutation handling is idempotent;
- invalid mutation input is rejected;
- remote conflict ordering follows the contract;
- active queries exclude tombstones while sync queries include them.

### Application Verification

```bash
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
./scripts/verify.sh
```

Run applicable desktop and Android compilation checks. Record iOS execution as `NOT RUN` only when the host limitation prevents it.

### Remote Verification Boundary

- The remote-service reviewer must approve the local migration, RLS or RPC changes, test evidence, and proposed hosted mutation.
- Obtain explicit human approval immediately before applying the exact hosted migration or policy change.
- After deployment, perform read-only verification of migration IDs, tables or functions, RLS status, permissions, and security-advisor results.
- Record safe identifiers only. Never include keys, passwords, tokens, or service-role credentials in evidence.

## 6. Constraints & Rules

- **Money:** Monetary values remain `Long` integer cents locally and remotely. Never use `Double` or `Float` for money.
- **Time:** Domain timestamps use `kotlin.time.Instant`. Sync ordering uses explicit epoch-millisecond values where required by the conflict tuple.
- **Local First:** UI reads only from Room. Network success is never required for a local user action to succeed.
- **Identity:** The remote server derives ownership from the authenticated session. The client must not be trusted to assign another user’s `userId`.
- **Remote Writes:** Remote mutation endpoints must enforce idempotency and conflict ordering. Direct client writes that bypass the protocol must not be used for synchronised entities.
- **Source-Controlled Backend:** All remote tables, policies, functions, permissions, and migrations remain version controlled under `supabase/migrations/`.
- **Deletion:** Claimed synchronised records use tombstones until remote confirmation. Unclaimed device-only records remain outside the sync protocol.
- **Architecture:** ViewModels use domain use cases; they do not perform Room, Supabase, outbox, or navigation work directly.
- **Secrets:** Only the public Supabase URL and publishable/anon key may reach approved runtime configuration. Service-role keys must never enter the app, repository, evidence, tests, or agent output.
- **Remote Governance:** Follow the `remote-service-change` workflow and `supabase-offline-sync` skill for every remote-service operation.

## 7. Authority Boundaries

- **Local Application Changes:** Application code, Room migrations, tests, and local Supabase migrations required for this contract are in scope.
- **Hosted Supabase Changes:** No hosted migration, RLS policy, RPC, permission, Auth setting, test user, or remote data change may occur without explicit human approval immediately before that exact action.
- **Git:** Do not stage, commit, push, or change remotes unless explicitly approved.
- **Scope:** Do not add background scheduling, realtime sync, new authentication methods, or unrelated product features during this slice.
