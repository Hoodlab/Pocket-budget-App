# Task Evidence: Secure Remote Data Model

## 1. Outcome

Established Pocket Budget's secure remote database foundation on PostgreSQL via version-controlled Supabase migrations, with strict multi-tenant Row Level Security (`auth.uid() = user_id`), domain integrity constraints, soft-delete tombstones, automated pgTAP security tests, and verified deployment to the hosted development project `pocket-budget-dev` (`hyetpnyruynichpsxyvz`).

## 2. Scope

### In-Scope Deliverables
- **Remote Tables:** `transactions`, `monthly_budgets`, and `category_budgets` defined in version-controlled migration [`supabase/migrations/20260916181116_create_secure_remote_schema.sql`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/supabase/migrations/20260916181116_create_secure_remote_schema.sql).
- **Authenticated Ownership:** Every row contains a non-null `user_id` referencing `auth.users(id) ON DELETE CASCADE`.
- **Row Level Security:** RLS enabled on all three tables with 12 distinct policies enforcing `auth.uid() = user_id` across `SELECT`, `INSERT`, `UPDATE`, and `DELETE`.
- **Anonymous Denial:** Role `anon` is granted zero permissions and denied all data access by default.
- **Domain Constraints:** PostgreSQL `CHECK` constraints enforcing positive integer cents (`amount > 0`, `limit_amount > 0`), non-blank descriptions, valid months `1..12`, valid years `1900..3000`, expense-only budget categories, and cross-column transaction category eligibility (`valid_category_for_type`).
- **Stable Identity:** Client-generated UUID primary keys for `transactions`, composite primary keys `(user_id, year, month)` and `(user_id, year, month, category)` for budgets.
- **Soft Deletes & Sync Preparation:** `deleted_at TIMESTAMPTZ DEFAULT NULL` for soft deletes, automated trigger `handle_updated_at()`, and partial/composite indexes supporting sync-delta queries.
- **Automated Database Tests:** Comprehensive pgTAP test suite in [`supabase/tests/01_security_and_constraints_test.sql`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/supabase/tests/01_security_and_constraints_test.sql) covering 25 security and constraint assertions.
- **Dedicated Reviewer:** Configured [`remote_service_reviewer`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/.agents/reviewers/remote-service.md) agent with 9-point checklist and post-deployment validation capability.
- **Hosted Deployment:** Deployed and verified against hosted development project `pocket-budget-dev` (`hyetpnyruynichpsxyvz`) following explicit human approval.

### Explicit Exclusions
- Application authentication screens and session management.
- Runtime Supabase client SDK integration or runtime configuration.
- Client-side synchronisation engine, mutation queues, or outbox.
- Local Room schema migrations for sync metadata.
- Realtime subscriptions, Edge Functions, or Storage.
- Production environment infrastructure or mutations.

## 3. Test-First Evidence (RED & GREEN)

### RED Phase (Failing Test)
- **Failing Test:** Initial pgTAP execution against local database prior to final test privileges and RLS syntax alignment in `supabase/tests/01_security_and_constraints_test.sql`.
- **Execution Command:**
  ```bash
  supabase test db --local
  ```
- **Observed Failure Output:**
  ```text
  Connecting to local database...
  psql:.../01_security_and_constraints_test.sql:33: ERROR:  permission denied for table transactions
  HINT:  Grant the required privileges to the current role with: GRANT SELECT ON public.transactions TO anon;
  CONTEXT:  SQL statement "SELECT * FROM public.transactions"
  PL/pgSQL function is_empty(text,text) line 9 at FOR over EXECUTE statement
  /Users/apple/.../01_security_and_constraints_test.sql .. 
  Dubious, test returned 3 (wstat 768, 0x300)
  Failed 16/22 subtests 

  Test Summary Report
  -------------------
  /Users/apple/.../01_security_and_constraints_test.sql (Wstat: 768 (exited 3) Tests: 6 Failed: 0)
    Non-zero exit status: 3
    Parse errors: Bad plan.  You planned 22 tests but ran 6.
  Files=1, Tests=6,  0 wallclock secs ( 0.03 usr  0.01 sys +  0.01 cusr  0.00 csys =  0.05 CPU)
  Result: FAIL
  error running container: exit 1
  ```

### GREEN Phase (Passing Test)
- **Implementation Files:**
  - Migration: [`supabase/migrations/20260916181116_create_secure_remote_schema.sql`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/supabase/migrations/20260916181116_create_secure_remote_schema.sql)
  - Test Suite: [`supabase/tests/01_security_and_constraints_test.sql`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/supabase/tests/01_security_and_constraints_test.sql)
- **Execution Command:**
  ```bash
  supabase test db --local
  ```
- **Observed Passing Output:**
  ```text
  Connecting to local database...
  /Users/apple/.../01_security_and_constraints_test.sql .. ok
  All tests successful.
  Files=1, Tests=25,  1 wallclock secs ( 0.04 usr  0.01 sys +  0.02 cusr  0.00 csys =  0.07 CPU)
  Result: PASS
  ```

## 4. Automated UI-Flow Test Coverage
- **Status**: Not Applicable (Backend remote schema slice; no UI flows were added or modified).

## 5. Visual-Validation Evidence
- **Status**: Not Applicable (Backend remote schema slice; no presentation components or Compose UI modified).

## 6. Verification Results

### Project Verification Gate
- **Execution Command:**
  ```bash
  ./scripts/verify.sh
  ```
- **Gate Summary:**
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

### Local Supabase Gate
- **Reset & Rebuild:** `supabase db reset --local --yes` — `PASS`
- **Schema Lint:** `supabase db lint --local` — `PASS` (0 schema errors)
- **pgTAP Test Suite:** `supabase test db --local` — `PASS` (25/25 tests passing)
- **PostgREST Schema Reflection:** `curl -s http://127.0.0.1:54321/rest/v1/` — `VERIFIED`

### Hosted Deployment & MCP Verification
- **Target Project:** `pocket-budget-dev` (`hyetpnyruynichpsxyvz`)
- **Environment:** `development` (Region: `eu-central-1`)
- **Deployment Mutation:** `apply_migration("create_secure_remote_schema")` — `SUCCESS`
- **Remote Migration Record:** Version `20260917150352`, Name `create_secure_remote_schema` (`PASS`)
- **Remote Tables & RLS:** Verified `rls_enabled: true` on `public.transactions`, `public.monthly_budgets`, `public.category_budgets` (`PASS`)
- **Security Advisors:** 0 tables without RLS, 0 missing RLS policies (`PASS`)

## 7. Execution Command Log
- `git diff --check`: `PASS`
- `supabase db reset --local --yes`: `PASS`
- `supabase db lint --local`: `PASS`
- `supabase test db --local`: `PASS` (25/25 tests)
- `./scripts/verify.sh`: `PASS`
- Supabase MCP `apply_migration`: `PASS`
- Supabase MCP `list_migrations`: `PASS`
- Supabase MCP `list_tables`: `PASS`
- Supabase MCP `get_advisors`: `PASS`

## 8. Limitations & Unverified Targets
- **iOS Simulator Tests**: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).

## 9. Final Checkpoint

### Reviewer Verdicts
- **Scope Reviewer:** `APPROVE`
- **Architecture Reviewer:** `APPROVE`
- **Domain Reviewer:** `APPROVE`
- **TDD & Evidence Reviewer:** `APPROVE`
- **Remote Service Reviewer:** `APPROVE`

### Proposed Commit Message
```text
feat(remote): establish secure remote database foundation and pgTAP tests

- Add version-controlled migration for transactions, monthly_budgets, and category_budgets
- Enforce strict Row Level Security with auth.uid() = user_id ownership policies
- Implement domain constraints for integer cents, non-blank descriptions, and category eligibility
- Add soft-delete tombstones and handle_updated_at trigger for future sync
- Add 25 automated pgTAP tests verifying RLS, anonymous rejection, and constraints
- Configure Remote Service Reviewer agent and document task evidence
- Apply and verify migration against hosted development project pocket-budget-dev
```
