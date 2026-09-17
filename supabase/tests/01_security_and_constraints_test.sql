BEGIN;
SELECT plan(25);

-- ==============================================================================
-- 1. Schema Structure & RLS Status
-- ==============================================================================
SELECT has_table('public', 'transactions', 'Table transactions should exist');
SELECT has_table('public', 'monthly_budgets', 'Table monthly_budgets should exist');
SELECT has_table('public', 'category_budgets', 'Table category_budgets should exist');

SELECT ok((SELECT relrowsecurity FROM pg_class WHERE relname = 'transactions'), 'RLS should be enabled on transactions');
SELECT ok((SELECT relrowsecurity FROM pg_class WHERE relname = 'monthly_budgets'), 'RLS should be enabled on monthly_budgets');
SELECT ok((SELECT relrowsecurity FROM pg_class WHERE relname = 'category_budgets'), 'RLS should be enabled on category_budgets');

-- ==============================================================================
-- Setup: Create Test Users in auth.users
-- ==============================================================================
INSERT INTO auth.users (id, instance_id, aud, role, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, created_at, updated_at)
VALUES 
    ('11111111-1111-1111-1111-111111111111', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'user_a@pocketbudget.test', 'encrypted', now(), '{"provider":"email","providers":["email"]}', '{}', now(), now()),
    ('22222222-2222-2222-2222-222222222222', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'user_b@pocketbudget.test', 'encrypted', now(), '{"provider":"email","providers":["email"]}', '{}', now(), now())
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 2. Anonymous Access Rejection
-- ==============================================================================
SET LOCAL ROLE anon;
SET LOCAL "request.jwt.claims" = '{"role": "anon"}';

SELECT throws_ok(
    'SELECT * FROM public.transactions',
    '42501',
    NULL,
    'Anonymous user should be denied SELECT on transactions'
);

SELECT throws_ok(
    $$ INSERT INTO public.transactions (id, user_id, description, amount, type, category, transaction_date)
       VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'Secret', 1000, 'EXPENSE', 'GROCERIES', now()) $$,
    '42501',
    NULL,
    'Anonymous user should be denied INSERT on transactions'
);

SELECT throws_ok(
    'UPDATE public.transactions SET description = ''Hacked''',
    '42501',
    NULL,
    'Anonymous user should be denied UPDATE on transactions'
);

SELECT throws_ok(
    'DELETE FROM public.transactions',
    '42501',
    NULL,
    'Anonymous user should be denied DELETE on transactions'
);

-- ==============================================================================
-- 3. Authenticated User A Can Create and Read Own Data
-- ==============================================================================
SET LOCAL ROLE authenticated;
SET LOCAL "request.jwt.claims" = '{"sub": "11111111-1111-1111-1111-111111111111", "role": "authenticated"}';

INSERT INTO public.transactions (id, user_id, description, amount, type, category, transaction_date)
VALUES ('aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'Supermarket', 4500, 'EXPENSE', 'GROCERIES', now());

INSERT INTO public.monthly_budgets (user_id, year, month, limit_amount)
VALUES ('11111111-1111-1111-1111-111111111111', 2026, 9, 250000);

INSERT INTO public.category_budgets (user_id, year, month, category, limit_amount)
VALUES ('11111111-1111-1111-1111-111111111111', 2026, 9, 'GROCERIES', 60000);

SELECT results_eq(
    'SELECT count(*)::integer FROM public.transactions WHERE user_id = ''11111111-1111-1111-1111-111111111111''',
    ARRAY[1],
    'User A should see 1 transaction belonging to themselves'
);

SELECT results_eq(
    'SELECT count(*)::integer FROM public.monthly_budgets WHERE user_id = ''11111111-1111-1111-1111-111111111111''',
    ARRAY[1],
    'User A should see 1 monthly budget belonging to themselves'
);

SELECT results_eq(
    'SELECT count(*)::integer FROM public.category_budgets WHERE user_id = ''11111111-1111-1111-1111-111111111111''',
    ARRAY[1],
    'User A should see 1 category budget belonging to themselves'
);

-- ==============================================================================
-- 4. Multi-Tenant User Isolation (User B cannot access User A's data)
-- ==============================================================================
SET LOCAL ROLE authenticated;
SET LOCAL "request.jwt.claims" = '{"sub": "22222222-2222-2222-2222-222222222222", "role": "authenticated"}';

SELECT is_empty(
    'SELECT * FROM public.transactions',
    'User B should not see any of User A transactions'
);

SELECT is_empty(
    'SELECT * FROM public.monthly_budgets',
    'User B should not see any of User A monthly budgets'
);

SELECT is_empty(
    'SELECT * FROM public.category_budgets',
    'User B should not see any of User A category budgets'
);

SELECT throws_ok(
    $$ INSERT INTO public.transactions (id, user_id, description, amount, type, category, transaction_date)
       VALUES ('bbbbbbbb-2222-2222-2222-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'Spoofed', 2000, 'EXPENSE', 'DINING', now()) $$,
    '42501',
    NULL,
    'User B cannot insert transactions with User A user_id'
);

-- User B attempts to update and delete User A's transaction
UPDATE public.transactions SET description = 'Hacked' WHERE id = 'aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa';
DELETE FROM public.transactions WHERE id = 'aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa';

-- Switch back to User A and verify transaction is completely intact
SET LOCAL ROLE authenticated;
SET LOCAL "request.jwt.claims" = '{"sub": "11111111-1111-1111-1111-111111111111", "role": "authenticated"}';

SELECT results_eq(
    'SELECT description FROM public.transactions WHERE id = ''aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa''',
    ARRAY['Supermarket'],
    'User A transaction remains unchanged despite User B update and delete attempts'
);

-- ==============================================================================
-- 5. Domain Constraints Validation (as User A)
-- ==============================================================================
-- Description cannot be blank
SELECT throws_ok(
    $$ INSERT INTO public.transactions (id, user_id, description, amount, type, category, transaction_date)
       VALUES ('aaaaaaaa-1111-1111-1111-aaaaaaaaaa01', '11111111-1111-1111-1111-111111111111', '   ', 1000, 'EXPENSE', 'GROCERIES', now()) $$,
    '23514',
    NULL,
    'Transaction description must not be blank'
);

-- Amount must be > 0
SELECT throws_ok(
    $$ INSERT INTO public.transactions (id, user_id, description, amount, type, category, transaction_date)
       VALUES ('aaaaaaaa-1111-1111-1111-aaaaaaaaaa02', '11111111-1111-1111-1111-111111111111', 'Zero Amount', 0, 'EXPENSE', 'GROCERIES', now()) $$,
    '23514',
    NULL,
    'Transaction amount must be strictly positive'
);

-- Invalid category for type (Income category used for Expense)
SELECT throws_ok(
    $$ INSERT INTO public.transactions (id, user_id, description, amount, type, category, transaction_date)
       VALUES ('aaaaaaaa-1111-1111-1111-aaaaaaaaaa03', '11111111-1111-1111-1111-111111111111', 'Salary as Expense', 1000, 'EXPENSE', 'SALARY', now()) $$,
    '23514',
    NULL,
    'Salary cannot be assigned to an EXPENSE transaction'
);

-- Month out of bounds
SELECT throws_ok(
    $$ INSERT INTO public.monthly_budgets (user_id, year, month, limit_amount)
       VALUES ('11111111-1111-1111-1111-111111111111', 2026, 13, 100000) $$,
    '23514',
    NULL,
    'Budget month must be between 1 and 12'
);

-- Non-expense category for category budget
SELECT throws_ok(
    $$ INSERT INTO public.category_budgets (user_id, year, month, category, limit_amount)
       VALUES ('11111111-1111-1111-1111-111111111111', 2026, 9, 'SALARY', 50000) $$,
    '23514',
    NULL,
    'Income category SALARY cannot have a budget limit'
);

-- ==============================================================================
-- 6. Soft Delete and Sync Retrieval
-- ==============================================================================
-- Soft delete the transaction
UPDATE public.transactions 
SET deleted_at = timezone('utc'::text, now()) 
WHERE id = 'aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa';

-- Active query excludes soft-deleted row
SELECT is_empty(
    'SELECT * FROM public.transactions WHERE deleted_at IS NULL',
    'Active queries should exclude soft-deleted transactions'
);

-- Sync query includes soft-deleted tombstone
SELECT results_eq(
    'SELECT count(*)::integer FROM public.transactions WHERE deleted_at IS NOT NULL',
    ARRAY[1],
    'Sync queries can retrieve soft-deleted tombstones'
);

SELECT * FROM finish();
ROLLBACK;
