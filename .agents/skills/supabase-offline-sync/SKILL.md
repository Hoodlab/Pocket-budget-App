---
name: supabase-offline-sync
description: Use when Pocket Budget adds or modifies Supabase authentication, remote persistence, Row Level Security, offline-first synchronization, sync queues, conflict handling, or Supabase MCP-assisted development.
---

# Supabase Offline Sync Skill

Use this skill when Pocket Budget integrates or modifies Supabase authentication, remote PostgreSQL persistence, Row Level Security (RLS), offline-first synchronization, or Supabase MCP-assisted development.

## Required Process & Governance
1. **Workflow Activation**: Always apply and follow the `remote-service-change` workflow (`.agents/workflows/remote-service-change.md`) for any Supabase operation.
2. **Read-Only Inspection First**: Use the connected Supabase MCP server in read-only mode to inspect existing tables, migrations, advisors, and auth settings before proposing any mutations.
3. **Explicit Human Approval**: Never infer write permission. Request explicit human approval immediately before applying Supabase migrations, updating RLS policies, or changing Auth settings.
4. **Safe Identifiers Only**: Report Supabase project status using safe project references (e.g. project reference ID) without exposing internal secrets or credentials.

## Pocket Budget Architecture Rules
1. **Room as Local Source of Truth**: The local Room database is the authoritative source of truth for the presentation layer. The UI must render immediately with 0ms network latency from Room, while sync runs asynchronously in the background.
2. **User Isolation & Row Level Security**:
  - Every remote row (`transactions`, `monthly_budgets`, `category_budgets`) must contain a `user_id` referencing `auth.users(id)`.
  - Enable RLS on all tables and define policies enforcing `auth.uid() = user_id` for SELECT, INSERT, UPDATE, and DELETE.
3. **Idempotency & Keys**:
  - Use client-generated UUIDs for transaction IDs and composite keys `(user_id, month, year)` / `(user_id, category, month, year)` for budgets.
  - Remote writes must be idempotent upserts.
4. **Conflict Handling & Resilience**:
  - Define and test Last-Write-Wins (LWW) conflict resolution using `updatedAt` / `updatedAtEpochMillis` only when explicitly approved in the task contract.
  - Retain failed local mutations in a persistent pending/sync state for retry; never silently drop un-synced data on network or server failure.
5. **Reproducible Migrations**:
  - Maintain all schema definitions, RLS policies, and triggers in version-controlled SQL files (e.g. `supabase/migrations/`). Do not rely on ad-hoc dashboard changes.

## Security & Secrets Guardrails
1. **Zero Service-Role Keys in Client**: Never place or commit Supabase service-role keys in client app source code, test files, configuration artifacts, or chat responses.
2. **Runtime Configuration vs. MCP Development**:
  - Distinguish development-time MCP tool connections (strictly for local/development assistance) from the app's runtime configuration.
  - Runtime client initialization uses only the Supabase project URL and anon public publishable key, loaded securely via build config / environment properties.
