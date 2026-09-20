BEGIN;
SELECT plan(19);

-- ==============================================================================
-- Setup: Ensure Test Users exist in auth.users
-- ==============================================================================
INSERT INTO auth.users (id, instance_id, aud, role, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, created_at, updated_at)
VALUES 
    ('11111111-1111-1111-1111-111111111111', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'sync_user_a@pocketbudget.test', 'encrypted', now(), '{"provider":"email","providers":["email"]}', '{}', now(), now()),
    ('22222222-2222-2222-2222-222222222222', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'sync_user_b@pocketbudget.test', 'encrypted', now(), '{"provider":"email","providers":["email"]}', '{}', now(), now())
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 1. Anonymous Access Rejection
-- ==============================================================================
SET LOCAL ROLE anon;
SET LOCAL "request.jwt.claims" = '{"role": "anon"}';

SELECT throws_ok(
    $$ SELECT public.apply_sync_mutations('[]'::jsonb) $$,
    '42501',
    NULL,
    'Anonymous user should be denied execution of apply_sync_mutations'
);

SELECT throws_ok(
    $$ SELECT public.pull_sync_changes(0) $$,
    '42501',
    NULL,
    'Anonymous user should be denied execution of pull_sync_changes'
);

-- ==============================================================================
-- 2. User A Applies Initial UPSERT Mutations
-- ==============================================================================
SET LOCAL ROLE authenticated;
SET LOCAL "request.jwt.claims" = '{"sub": "11111111-1111-1111-1111-111111111111", "role": "authenticated"}';

SELECT results_eq(
    $$
    SELECT public.apply_sync_mutations(
        jsonb_build_array(
            jsonb_build_object(
                'mutationId', 'mut-tx-1',
                'deviceId', 'device-user-a',
                'entityType', 'TRANSACTION',
                'entityId', '33333333-3333-3333-3333-333333333333',
                'operation', 'UPSERT',
                'payload', jsonb_build_object(
                    'id', '33333333-3333-3333-3333-333333333333',
                    'description', 'Weekly Groceries',
                    'amount', 7500,
                    'type', 'EXPENSE',
                    'category', 'GROCERIES',
                    'dateEpochMillis', 1717171717000
                ),
                'updatedAtEpochMillis', 1000
            ),
            jsonb_build_object(
                'mutationId', 'mut-mb-1',
                'deviceId', 'device-user-a',
                'entityType', 'MONTHLY_BUDGET',
                'entityId', '2026-9',
                'operation', 'UPSERT',
                'payload', jsonb_build_object(
                    'year', 2026,
                    'month', 9,
                    'limitAmount', 300000
                ),
                'updatedAtEpochMillis', 1000
            ),
            jsonb_build_object(
                'mutationId', 'mut-cb-1',
                'deviceId', 'device-user-a',
                'entityType', 'CATEGORY_BUDGET',
                'entityId', '2026-9-GROCERIES',
                'operation', 'UPSERT',
                'payload', jsonb_build_object(
                    'year', 2026,
                    'month', 9,
                    'category', 'GROCERIES',
                    'limitAmount', 80000
                ),
                'updatedAtEpochMillis', 1000
            )
        )
    ) -> 0 ->> 'status'
    $$,
    ARRAY['APPLIED'],
    'Transaction mutation should be APPLIED'
);

-- Verify records exist in DB
SELECT results_eq(
    $$ SELECT count(*)::integer FROM public.transactions WHERE id = '33333333-3333-3333-3333-333333333333' AND user_id = '11111111-1111-1111-1111-111111111111' $$,
    ARRAY[1],
    'Transaction record should exist for User A'
);

SELECT results_eq(
    $$ SELECT count(*)::integer FROM public.monthly_budgets WHERE user_id = '11111111-1111-1111-1111-111111111111' AND year = 2026 AND month = 9 $$,
    ARRAY[1],
    'Monthly budget record should exist for User A'
);

SELECT results_eq(
    $$ SELECT count(*)::integer FROM public.category_budgets WHERE user_id = '11111111-1111-1111-1111-111111111111' AND year = 2026 AND month = 9 AND category = 'GROCERIES' $$,
    ARRAY[1],
    'Category budget record should exist for User A'
);

-- ==============================================================================
-- 3. Idempotent Retry: Same Mutation IDs Return APPLIED Without Duplicating
-- ==============================================================================
SELECT results_eq(
    $$
    SELECT public.apply_sync_mutations(
        jsonb_build_array(
            jsonb_build_object(
                'mutationId', 'mut-tx-1',
                'deviceId', 'device-user-a',
                'entityType', 'TRANSACTION',
                'entityId', '33333333-3333-3333-3333-333333333333',
                'operation', 'UPSERT',
                'payload', jsonb_build_object(
                    'id', '33333333-3333-3333-3333-333333333333',
                    'description', 'Weekly Groceries',
                    'amount', 7500,
                    'type', 'EXPENSE',
                    'category', 'GROCERIES',
                    'dateEpochMillis', 1717171717000
                ),
                'updatedAtEpochMillis', 1000
            )
        )
    ) -> 0 ->> 'status'
    $$,
    ARRAY['APPLIED'],
    'Retrying exact same mutationId should return APPLIED (idempotent)'
);

SELECT results_eq(
    $$ SELECT count(*)::integer FROM public.transactions WHERE id = '33333333-3333-3333-3333-333333333333' $$,
    ARRAY[1],
    'Still exactly 1 transaction record after idempotent retry'
);

-- ==============================================================================
-- 4. Cross-User Mutation Rejected: User B Cannot Mutate User A's Transaction
-- ==============================================================================
SET LOCAL ROLE authenticated;
SET LOCAL "request.jwt.claims" = '{"sub": "22222222-2222-2222-2222-222222222222", "role": "authenticated"}';

SELECT results_eq(
    $$
    SELECT public.apply_sync_mutations(
        jsonb_build_array(
            jsonb_build_object(
                'mutationId', 'mut-hack-1',
                'deviceId', 'device-user-b',
                'entityType', 'TRANSACTION',
                'entityId', '33333333-3333-3333-3333-333333333333',
                'operation', 'UPSERT',
                'payload', jsonb_build_object(
                    'id', '33333333-3333-3333-3333-333333333333',
                    'description', 'Hacked Transaction',
                    'amount', 999999,
                    'type', 'EXPENSE',
                    'category', 'OTHER',
                    'dateEpochMillis', 1717171717000
                ),
                'updatedAtEpochMillis', 9999
            )
        )
    ) -> 0 ->> 'status'
    $$,
    ARRAY['REJECTED'],
    'User B cannot mutate User A transaction (cross-user rejected)'
);

-- ==============================================================================
-- 5. Invalid Input Validation
-- ==============================================================================
SET LOCAL ROLE authenticated;
SET LOCAL "request.jwt.claims" = '{"sub": "11111111-1111-1111-1111-111111111111", "role": "authenticated"}';

SELECT results_eq(
    $$
    SELECT public.apply_sync_mutations(
        jsonb_build_array(
            jsonb_build_object(
                'mutationId', '',
                'deviceId', 'device-a',
                'entityType', 'TRANSACTION',
                'entityId', '33333333-3333-3333-3333-333333333333',
                'operation', 'INVALID_OP',
                'payload', '{}'::jsonb,
                'updatedAtEpochMillis', 1000
            )
        )
    ) -> 0 ->> 'status'
    $$,
    ARRAY['REJECTED'],
    'Mutation with invalid operation or missing fields must be REJECTED'
);

-- ==============================================================================
-- 6. Last-Write-Wins Conflict Resolution
-- ==============================================================================
-- 6a. Stale mutation (updatedAtEpochMillis = 500 < existing 1000) should be STALE
SELECT results_eq(
    $$
    SELECT public.apply_sync_mutations(
        jsonb_build_array(
            jsonb_build_object(
                'mutationId', 'mut-stale-1',
                'deviceId', 'device-user-a-old',
                'entityType', 'TRANSACTION',
                'entityId', '33333333-3333-3333-3333-333333333333',
                'operation', 'UPSERT',
                'payload', jsonb_build_object(
                    'id', '33333333-3333-3333-3333-333333333333',
                    'description', 'Stale Groceries',
                    'amount', 1000,
                    'type', 'EXPENSE',
                    'category', 'GROCERIES',
                    'dateEpochMillis', 1717171717000
                ),
                'updatedAtEpochMillis', 500
            )
        )
    ) -> 0 ->> 'status'
    $$,
    ARRAY['STALE'],
    'Older mutation must return STALE and not overwrite'
);

-- Verify existing description did not change
SELECT results_eq(
    $$ SELECT description FROM public.transactions WHERE id = '33333333-3333-3333-3333-333333333333' $$,
    ARRAY['Weekly Groceries'],
    'Stale mutation did not overwrite transaction description'
);

-- 6b. Newer mutation (updatedAtEpochMillis = 2000 > existing 1000) should be APPLIED
SELECT results_eq(
    $$
    SELECT public.apply_sync_mutations(
        jsonb_build_array(
            jsonb_build_object(
                'mutationId', 'mut-newer-1',
                'deviceId', 'device-user-a',
                'entityType', 'TRANSACTION',
                'entityId', '33333333-3333-3333-3333-333333333333',
                'operation', 'UPSERT',
                'payload', jsonb_build_object(
                    'id', '33333333-3333-3333-3333-333333333333',
                    'description', 'Updated Groceries Newer',
                    'amount', 8500,
                    'type', 'EXPENSE',
                    'category', 'GROCERIES',
                    'dateEpochMillis', 1717171717000
                ),
                'updatedAtEpochMillis', 2000
            )
        )
    ) -> 0 ->> 'status'
    $$,
    ARRAY['APPLIED'],
    'Newer mutation must return APPLIED'
);

SELECT results_eq(
    $$ SELECT description FROM public.transactions WHERE id = '33333333-3333-3333-3333-333333333333' $$,
    ARRAY['Updated Groceries Newer'],
    'Newer mutation successfully updated transaction description'
);

-- ==============================================================================
-- 7. Soft Delete Tombstones & Active Query Isolation
-- ==============================================================================
SELECT results_eq(
    $$
    SELECT public.apply_sync_mutations(
        jsonb_build_array(
            jsonb_build_object(
                'mutationId', 'mut-del-1',
                'deviceId', 'device-user-a',
                'entityType', 'TRANSACTION',
                'entityId', '33333333-3333-3333-3333-333333333333',
                'operation', 'DELETE',
                'payload', '{}'::jsonb,
                'updatedAtEpochMillis', 3000
            )
        )
    ) -> 0 ->> 'status'
    $$,
    ARRAY['APPLIED'],
    'DELETE mutation should be APPLIED'
);

-- Active query (deleted_at IS NULL) must NOT return the soft-deleted row
SELECT is_empty(
    $$ SELECT * FROM public.transactions WHERE id = '33333333-3333-3333-3333-333333333333' AND deleted_at IS NULL $$,
    'Active query must exclude soft-deleted transaction tombstone'
);

-- Physical row still exists with deleted_at set
SELECT isnt_empty(
    $$ SELECT * FROM public.transactions WHERE id = '33333333-3333-3333-3333-333333333333' AND deleted_at IS NOT NULL $$,
    'Tombstone row still exists with deleted_at NOT NULL'
);

-- ==============================================================================
-- 8. Pull Sync Changes
-- ==============================================================================
-- Pulling changes since epoch 0 should return the soft-deleted transaction tombstone
SELECT results_eq(
    $$
    SELECT jsonb_array_length(public.pull_sync_changes(0) -> 'transactions')
    $$,
    ARRAY[1],
    'pull_sync_changes includes tombstones for sync reconciliation'
);

SELECT ok(
    (SELECT (public.pull_sync_changes(0) -> 'transactions' -> 0 ->> 'deletedAtEpochMillis') IS NOT NULL),
    'Pulled transaction tombstone has deletedAtEpochMillis set'
);

SELECT * FROM finish();
ROLLBACK;
