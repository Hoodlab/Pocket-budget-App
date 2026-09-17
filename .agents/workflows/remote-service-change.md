---
description: Reusable workflow for any task reading or modifying an external backend, cloud database, authentication provider, or remote API.
---

# Remote Service and External Change Workflow

Use this workflow for any task that reads or modifies an external backend, cloud database, authentication provider, or remote API.

## 1. Read-Only Audit and Environment Classification
1. Perform a read-only audit of the target external project/service before proposing or executing any mutations.
2. Record the environment classification in task evidence: `local`, `development`, `staging`, or `production`.
3. Never use production data or production credentials for agent-driven development or testing unless the human user explicitly authorizes that exact environment.

## 2. Explicit Human Approval at Point of External Mutation
1. Never infer permission for external writes from an ordinary coding or design task.
2. Request and obtain explicit human approval immediately before executing any of the following external mutations:
  - Creating or modifying remote database schemas or tables.
  - Creating or modifying Row Level Security (RLS) or access control policies.
  - Modifying authentication provider or service settings.
  - Deleting, seeding, or modifying remote data.
  - Deploying remote functions, edge functions, or serverless scripts.

## 3. Source-Controlled Migrations and Policies
1. Every remote schema change and access-policy modification must be defined in source-controlled migration/policy files in the repository.
2. Never make dashboard-only or ad-hoc remote changes that cannot be deterministically reproduced from repository source files.

## 4. Architecture and Contract Definition
Define the following specifications before beginning implementation:
- **Identity & Ownership Model**: How users authenticate, how user IDs are propagated, and how row/resource ownership is established.
- **Remote Data Contract**: Strict types, nullable fields, timestamp conventions, and currency/unit representations.
- **Deletion Behavior**: Soft delete tombstones vs. hard deletes, propagation of deletions across local and remote storage.
- **Idempotency Method**: Client-generated unique IDs or idempotency keys to guarantee duplicate submissions do not duplicate records remotely.
- **Offline-First Protocol**: Local storage remaining the authoritative source of truth for immediate reads and writes.
- **Conflict Resolution Policy**: Explicit strategy (e.g. Last-Write-Wins based on timestamps) when concurrent mutations occur.

## 5. Required Test Coverage Matrix
Implement automated tests verifying:
1. **Unauthenticated access rejected**: Requests without valid authentication credentials fail.
2. **User data isolation**: One authenticated user cannot read, insert, update, or delete another user's records.
3. **Offline local write**: Mutations succeed locally without network connectivity.
4. **Eventual successful sync**: Queued local mutations synchronize successfully once connectivity is available.
5. **Idempotent retry**: Retrying an operation with the same idempotency key does not duplicate records remotely.
6. **Failure resilience**: Network or remote errors retain local operations in a recoverable state for subsequent retry.
7. **Declared conflict behavior**: Conflicting concurrent updates resolve strictly according to the defined policy.

## 6. Remote Verification Evidence & Secrets Protection
1. Record remote verification evidence using safe identifiers only (environment, safe project identifier/reference, migration IDs, RLS check results, and exact verification tools/commands used).
2. Never log, commit, or expose secret API keys, service-role tokens, or private credentials in diffs, evidence files, test fixtures, or chat responses.
3. Mark unavailable remote verification explicitly as `NOT RUN` with the real constraint; do not treat local mocks or tests as proof of remote backend access policies.
