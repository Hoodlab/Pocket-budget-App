# Implementation Contract: Secure Remote Data Model

## 1. User Outcome

- A signed-in user’s remote transactions, monthly budgets, and category budgets belong only to that user.
- An unauthenticated request cannot read or modify user-owned remote data.
- One authenticated user cannot read, insert, update, or delete another user’s remote data.
- The remote database stores the same core financial facts as the local application.
- Remote data is ready for future offline-first synchronisation without changing the current local-first user experience.
- Every remote schema and security rule can be recreated from source-controlled migration files.

## 2. In-Scope Behavior

- **Remote Transactions Table:** A remote `transactions` table stores transaction ID, authenticated user ownership, description, amount in integer cents, transaction type, category, date, timestamps, and soft-delete state.
- **Remote Monthly Budgets Table:** A remote `monthly_budgets` table stores one total monthly budget per authenticated user, month, and year.
- **Remote Category Budgets Table:** A remote `category_budgets` table stores one category allocation per authenticated user, eligible expense category, month, and year.
- **Authenticated Ownership:** Every application row includes a non-null `user_id` referencing `auth.users(id)`.
- **Row Level Security:** RLS is enabled on every application table.
- **User Isolation Policies:** Database policies enforce `auth.uid() = user_id` for `SELECT`, `INSERT`, `UPDATE`, and `DELETE`.
- **Remote Data Constraints:** Database constraints enforce valid money, transaction type, category values, month/year ranges, and required fields.
- **Stable Record Identity:** Transactions use stable client-generated IDs. Monthly and category budgets use user-scoped natural or composite identity that prevents duplicates for the same period.
- **Sync Preparation Fields:** Remote records include the minimum timestamps and soft-delete fields required for a later offline-first sync contract.
- **Soft Deletes:** Deleted records retain a deletion timestamp for future synchronisation rather than being immediately removed from the remote database.
- **Indexes:** User-scoped indexes support efficient future sync queries by ownership and update time.
- **Source-Controlled Migrations:** Schema, constraints, indexes, RLS enablement, and policies are defined only in version-controlled files under `supabase/migrations/`.
- **Local Database Tests:** Local Supabase tests verify schema, constraints, unauthenticated rejection, user isolation, and soft-delete behaviour.
- **Hosted Deployment:** The tested migration is applied to the hosted development project only after explicit user approval.

## 3. Explicit Exclusions

- Pocket Budget application authentication screens or session management.
- Application runtime Supabase configuration.
- Application-to-cloud synchronisation.
- Local Room schema changes for sync metadata or outbox records.
- Conflict resolution, mutation retries, or sync queues.
- Realtime subscriptions, Edge Functions, Storage, files, notifications, analytics, or reports.
- Hosted migration deployment without explicit approval immediately before the mutation.
- Production environment changes.

## 4. Acceptance Criteria

- [ ] Remote `transactions`, `monthly_budgets`, and `category_budgets` tables are created through version-controlled migration files.
- [ ] Every application table has a non-null `user_id` referencing `auth.users(id)`.
- [ ] Row Level Security is enabled on every application table.
- [ ] Unauthenticated requests cannot select, insert, update, or delete application data.
- [ ] An authenticated user can access only rows whose `user_id` equals `auth.uid()`.
- [ ] An authenticated user cannot insert or update a row with another user’s ID.
- [ ] Transaction amounts use PostgreSQL `BIGINT` and reject invalid monetary values.
- [ ] Transaction type values are restricted to the application’s canonical `INCOME` and `EXPENSE` values.
- [ ] Transaction category values are restricted to the application’s predefined categories, including `UNCATEGORIZED`.
- [ ] Transaction date, created timestamp, updated timestamp, and soft-delete timestamp use the contract’s declared integer epoch-millis convention.
- [ ] A monthly budget is unique for one user, month, and year.
- [ ] A category budget is unique for one user, eligible category, month, and year.
- [ ] Income categories and `UNCATEGORIZED` cannot be stored as category-budget allocations.
- [ ] Category and monthly budget amounts use `BIGINT` integer cents and reject invalid values.
- [ ] Soft-deleted records are excluded from active-data queries while remaining available for future sync-delta queries.
- [ ] User-and-update-time indexes exist for future synchronisation queries.
- [ ] A clean local `supabase db reset` recreates the schema successfully.
- [ ] Local database tests prove unauthenticated rejection, user isolation, invalid-data rejection, and soft-delete behaviour.
- [ ] No Pocket Budget application code, Gradle configuration, runtime credentials, or hosted Supabase project is changed before explicit approval.

## 5. Verification Plan

### Local Database Tests

- **Test-First Evidence:** Provide genuine RED and GREEN evidence for at least one new security or data-integrity behaviour before finalising the migration.
- **Schema Tests:** Verify the three application tables, required columns, primary keys, foreign keys, constraints, and indexes exist after a clean reset.
- **Unauthenticated Access Tests:** Verify anonymous requests cannot read or modify application rows.
- **User-Isolation Tests:** Verify one authenticated test user cannot read, insert, update, or delete another authenticated test user’s rows.
- **Ownership-Spoofing Tests:** Verify an authenticated user cannot create or update a row with another user’s `user_id`.
- **Data-Integrity Tests:** Verify invalid transaction amounts, invalid transaction types, invalid categories, invalid month/year values, and ineligible category-budget categories are rejected.
- **Soft-Delete Tests:** Verify active queries exclude tombstones and future sync-delta queries can retrieve them.
- **Migration Reproducibility:** Verify all tests pass after a clean local database reset.

### Local Supabase Commands

```bash
supabase db reset
supabase test db
```

Use the configured local validation command where the project’s test setup requires it. Record exact commands and results in task evidence.

### Hosted Verification

After local tests pass and the user explicitly approves deployment:

- Apply only the reviewed migration files to the hosted **development** project.
- Use the Supabase MCP connection in read-only mode after deployment to verify:
  - migration IDs;
  - application table names;
  - RLS enabled status;
  - security-advisor results.
- Record the environment as `development`, safe project reference, migration IDs, and verification result.
- Do not expose credentials, keys, tokens, or sensitive user data.

### Manual Verification Journey

1. Start the local Supabase stack.
2. Reset the local database from the migration files.
3. Run the local database test suite.
4. Verify unauthenticated access is rejected.
5. Create two local authenticated test identities.
6. Insert User A’s transaction, monthly budget, and category budget.
7. Verify User A can access their own rows.
8. Verify User B cannot read or modify User A’s rows.
9. Verify invalid remote data is rejected by database constraints.
10. Verify soft-deleted records are hidden from active results but remain available to a sync-delta query.
11. Request explicit approval before applying the tested migration to the hosted development project.

## 6. Constraints & Rules

- **Remote-Service Governance:** Follow `.agents/workflows/remote-service-change.md` and `.agents/skills/supabase-offline-sync/SKILL.md`.
- **Local-First Development:** Build and test migrations, policies, and database tests locally before proposing a hosted mutation.
- **Explicit Approval:** The hosted development project remains read-only until the user explicitly approves the exact remote mutation immediately before it occurs.
- **Source-Controlled Changes:** Do not make schema, RLS, or policy changes only through Supabase Studio or another dashboard.
- **Ownership:** Database ownership must be enforced through `auth.uid() = user_id`; client-side filtering is not a security boundary.
- **Monetary Values:** Store and calculate all monetary values as `BIGINT` integer cents. Do not use floating-point columns for money.
- **Date and Time:** Store remote timestamps as epoch milliseconds to match the local data contract.
- **No Derived Records:** Do not store cash-flow totals, budget progress, remaining amounts, or health states as remote source-of-truth records.
- **Secrets:** Never place service-role keys, private credentials, MCP credentials, or tokens in client code, tests, evidence, migration files, or repository configuration.
- **Runtime Configuration:** Runtime clients may later use only the public Supabase URL and anon/publishable key through approved configuration.
- **No Application Integration Yet:** Do not add Supabase client dependencies, authentication UI, remote repositories, or synchronisation code in this slice.

## 7. Authority Boundaries

- **File Modifications:** Limited to local Supabase configuration, source-controlled migrations, local database tests, task evidence, and project-control files necessary for this slice.
- **Local Service Changes:** Starting, stopping, resetting, and testing the local Supabase stack is permitted.
- **Hosted Service Changes:** Creating the hosted development project and applying migrations require explicit user approval immediately before each remote mutation.
- **Git Commits:** Create an atomic commit only when explicitly requested and approved by the user.
- **Git Remotes:** Do not push, configure, or otherwise alter Git remotes.
- **Production:** Do not inspect, modify, or test against production Supabase data or infrastructure.
