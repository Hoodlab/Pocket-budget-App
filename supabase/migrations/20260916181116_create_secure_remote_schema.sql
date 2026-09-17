-- ==============================================================================
-- Pocket Budget: Secure Remote Database Foundation
-- Tables: transactions, monthly_budgets, category_budgets
-- Features: User ownership, RLS, domain constraints, soft deletes, triggers
-- ==============================================================================

-- 1. Helper function for updating 'updated_at' timestamps automatically
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ==============================================================================
-- Table: transactions
-- ==============================================================================
CREATE TABLE public.transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    description TEXT NOT NULL CHECK (length(trim(description)) > 0),
    amount BIGINT NOT NULL CHECK (amount > 0),
    type TEXT NOT NULL CHECK (type IN ('EXPENSE', 'INCOME')),
    category TEXT NOT NULL CHECK (category IN (
        'GROCERIES', 'DINING', 'TRANSPORT', 'SHOPPING', 'BILLS', 'ENTERTAINMENT',
        'SALARY', 'FREELANCE', 'INVESTMENT', 'GIFT',
        'UNCATEGORIZED', 'OTHER'
    )),
    transaction_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    CONSTRAINT valid_category_for_type CHECK (
        (type = 'EXPENSE' AND category IN ('GROCERIES', 'DINING', 'TRANSPORT', 'SHOPPING', 'BILLS', 'ENTERTAINMENT', 'UNCATEGORIZED', 'OTHER'))
        OR
        (type = 'INCOME' AND category IN ('SALARY', 'FREELANCE', 'INVESTMENT', 'GIFT', 'UNCATEGORIZED', 'OTHER'))
    )
);

CREATE TRIGGER set_transactions_updated_at
    BEFORE UPDATE ON public.transactions
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

CREATE INDEX idx_transactions_user_date ON public.transactions (user_id, transaction_date DESC);
CREATE INDEX idx_transactions_sync ON public.transactions (user_id, updated_at DESC);
CREATE INDEX idx_transactions_active ON public.transactions (user_id, transaction_date DESC) WHERE deleted_at IS NULL;

-- ==============================================================================
-- Table: monthly_budgets
-- ==============================================================================
CREATE TABLE public.monthly_budgets (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    year INTEGER NOT NULL CHECK (year BETWEEN 1900 AND 3000),
    month SMALLINT NOT NULL CHECK (month BETWEEN 1 AND 12),
    limit_amount BIGINT NOT NULL CHECK (limit_amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    PRIMARY KEY (user_id, year, month)
);

CREATE TRIGGER set_monthly_budgets_updated_at
    BEFORE UPDATE ON public.monthly_budgets
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

CREATE INDEX idx_monthly_budgets_sync ON public.monthly_budgets (user_id, updated_at DESC);

-- ==============================================================================
-- Table: category_budgets
-- ==============================================================================
CREATE TABLE public.category_budgets (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    year INTEGER NOT NULL CHECK (year BETWEEN 1900 AND 3000),
    month SMALLINT NOT NULL CHECK (month BETWEEN 1 AND 12),
    category TEXT NOT NULL CHECK (category IN ('GROCERIES', 'DINING', 'TRANSPORT', 'SHOPPING', 'BILLS', 'ENTERTAINMENT')),
    limit_amount BIGINT NOT NULL CHECK (limit_amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    PRIMARY KEY (user_id, year, month, category)
);

CREATE TRIGGER set_category_budgets_updated_at
    BEFORE UPDATE ON public.category_budgets
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

CREATE INDEX idx_category_budgets_sync ON public.category_budgets (user_id, updated_at DESC);

-- ==============================================================================
-- Permissions (Grants)
-- ==============================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON public.transactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.monthly_budgets TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.category_budgets TO authenticated;

-- ==============================================================================
-- Row Level Security (RLS)
-- ==============================================================================
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.monthly_budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.category_budgets ENABLE ROW LEVEL SECURITY;

-- Transactions Policies
CREATE POLICY "transactions_select_policy"
    ON public.transactions
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "transactions_insert_policy"
    ON public.transactions
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "transactions_update_policy"
    ON public.transactions
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "transactions_delete_policy"
    ON public.transactions
    FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

-- Monthly Budgets Policies
CREATE POLICY "monthly_budgets_select_policy"
    ON public.monthly_budgets
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "monthly_budgets_insert_policy"
    ON public.monthly_budgets
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "monthly_budgets_update_policy"
    ON public.monthly_budgets
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "monthly_budgets_delete_policy"
    ON public.monthly_budgets
    FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

-- Category Budgets Policies
CREATE POLICY "category_budgets_select_policy"
    ON public.category_budgets
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "category_budgets_insert_policy"
    ON public.category_budgets
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "category_budgets_update_policy"
    ON public.category_budgets
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "category_budgets_delete_policy"
    ON public.category_budgets
    FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);
