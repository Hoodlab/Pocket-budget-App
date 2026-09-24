-- ==============================================================================
-- Pocket Budget: Offline-First Sync Protocol Migration
-- Adds sync tracking columns, LWW conflict resolution function,
-- and RPCs: apply_sync_mutations, pull_sync_changes
-- ==============================================================================

-- 1. Add sync columns to transactions
ALTER TABLE public.transactions
    ADD COLUMN IF NOT EXISTS device_id TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS mutation_id TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS updated_at_epoch_millis BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM timezone('utc'::text, now())) * 1000)::BIGINT;

CREATE INDEX IF NOT EXISTS idx_transactions_sync_epoch
    ON public.transactions (user_id, updated_at_epoch_millis ASC);

-- 2. Add sync columns to monthly_budgets
ALTER TABLE public.monthly_budgets
    ADD COLUMN IF NOT EXISTS device_id TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS mutation_id TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS updated_at_epoch_millis BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM timezone('utc'::text, now())) * 1000)::BIGINT;

CREATE INDEX IF NOT EXISTS idx_monthly_budgets_sync_epoch
    ON public.monthly_budgets (user_id, updated_at_epoch_millis ASC);

-- 3. Add sync columns to category_budgets
ALTER TABLE public.category_budgets
    ADD COLUMN IF NOT EXISTS device_id TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS mutation_id TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS updated_at_epoch_millis BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM timezone('utc'::text, now())) * 1000)::BIGINT;

CREATE INDEX IF NOT EXISTS idx_category_budgets_sync_epoch
    ON public.category_budgets (user_id, updated_at_epoch_millis ASC);

-- 4. Deterministic Last-Write-Wins conflict resolution function
-- Ordering tuple: (updated_at_epoch_millis, device_id, mutation_id)
CREATE OR REPLACE FUNCTION public.is_mutation_winner(
    p_incoming_updated_at BIGINT,
    p_incoming_device_id TEXT,
    p_incoming_mutation_id TEXT,
    p_existing_updated_at BIGINT,
    p_existing_device_id TEXT,
    p_existing_mutation_id TEXT
) RETURNS BOOLEAN
LANGUAGE plpgsql
IMMUTABLE
SET search_path = public
AS $$
BEGIN
    IF p_incoming_updated_at <> p_existing_updated_at THEN
        RETURN p_incoming_updated_at > p_existing_updated_at;
    END IF;
    IF p_incoming_device_id <> p_existing_device_id THEN
        RETURN p_incoming_device_id > p_existing_device_id;
    END IF;
    RETURN p_incoming_mutation_id > p_existing_mutation_id;
END;
$$;

-- 5. RPC: apply_sync_mutations
CREATE OR REPLACE FUNCTION public.apply_sync_mutations(p_mutations JSONB)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
    v_user_id UUID;
    v_results JSONB := '[]'::jsonb;
    v_mutation JSONB;
    v_mutation_id TEXT;
    v_device_id TEXT;
    v_entity_type TEXT;
    v_entity_id TEXT;
    v_operation TEXT;
    v_payload JSONB;
    v_updated_at_epoch BIGINT;
    
    -- Transaction variables
    v_tx_id UUID;
    v_tx_desc TEXT;
    v_tx_amount BIGINT;
    v_tx_type TEXT;
    v_tx_category TEXT;
    v_tx_date TIMESTAMPTZ;
    v_existing_tx RECORD;
    
    -- Monthly Budget variables
    v_mb_year INT;
    v_mb_month INT;
    v_mb_limit BIGINT;
    v_existing_mb RECORD;
    
    -- Category Budget variables
    v_cb_year INT;
    v_cb_month INT;
    v_cb_category TEXT;
    v_cb_limit BIGINT;
    v_existing_cb RECORD;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthenticated request' USING ERRCODE = '42501';
    END IF;

    IF p_mutations IS NULL OR jsonb_typeof(p_mutations) <> 'array' THEN
        RETURN '[]'::jsonb;
    END IF;

    FOR v_mutation IN SELECT * FROM jsonb_array_elements(p_mutations)
    LOOP
        v_mutation_id := v_mutation->>'mutationId';
        v_device_id := v_mutation->>'deviceId';
        v_entity_type := v_mutation->>'entityType';
        v_entity_id := v_mutation->>'entityId';
        v_operation := v_mutation->>'operation';
        v_payload := v_mutation->'payload';
        v_updated_at_epoch := (v_mutation->>'updatedAtEpochMillis')::BIGINT;

        -- Validate required mutation fields
        IF v_mutation_id IS NULL OR v_mutation_id = '' OR
           v_device_id IS NULL OR v_device_id = '' OR
           v_entity_type IS NULL OR v_entity_type = '' OR
           v_operation IS NULL OR v_operation NOT IN ('UPSERT', 'DELETE') OR
           v_updated_at_epoch IS NULL THEN
            v_results := v_results || jsonb_build_object(
                'mutationId', COALESCE(v_mutation_id, ''),
                'status', 'REJECTED',
                'message', 'Invalid mutation header or missing fields'
            );
            CONTINUE;
        END IF;

        -- Process by entity type
        CASE v_entity_type
            WHEN 'TRANSACTION' THEN
                BEGIN
                    v_tx_id := (v_payload->>'id')::UUID;
                    IF v_tx_id IS NULL THEN
                        v_tx_id := v_entity_id::UUID;
                    END IF;
                EXCEPTION WHEN OTHERS THEN
                    v_results := v_results || jsonb_build_object(
                        'mutationId', v_mutation_id,
                        'status', 'REJECTED',
                        'message', 'Invalid transaction UUID'
                    );
                    CONTINUE;
                END;

                SELECT * INTO v_existing_tx FROM public.transactions WHERE id = v_tx_id;
                IF FOUND THEN
                    IF v_existing_tx.user_id <> v_user_id THEN
                        v_results := v_results || jsonb_build_object(
                            'mutationId', v_mutation_id,
                            'status', 'REJECTED',
                            'message', 'Cross-user modification denied'
                        );
                        CONTINUE;
                    END IF;

                    -- Idempotency check: same mutation_id was already applied
                    IF v_existing_tx.mutation_id = v_mutation_id THEN
                        v_results := v_results || jsonb_build_object(
                            'mutationId', v_mutation_id,
                            'status', 'APPLIED',
                            'message', 'Already applied'
                        );
                        CONTINUE;
                    END IF;

                    -- Last-Write-Wins conflict resolution
                    IF NOT public.is_mutation_winner(
                        v_updated_at_epoch, v_device_id, v_mutation_id,
                        v_existing_tx.updated_at_epoch_millis, v_existing_tx.device_id, v_existing_tx.mutation_id
                    ) THEN
                        v_results := v_results || jsonb_build_object(
                            'mutationId', v_mutation_id,
                            'status', 'STALE',
                            'message', 'Incoming mutation superseded by newer remote state'
                        );
                        CONTINUE;
                    END IF;
                END IF;

                IF v_operation = 'UPSERT' THEN
                    v_tx_desc := trim(COALESCE(v_payload->>'description', ''));
                    v_tx_amount := (v_payload->>'amount')::BIGINT;
                    v_tx_type := v_payload->>'type';
                    v_tx_category := v_payload->>'category';
                    v_tx_date := to_timestamp((v_payload->>'dateEpochMillis')::DOUBLE PRECISION / 1000.0);

                    IF v_tx_desc = '' OR v_tx_amount IS NULL OR v_tx_amount <= 0 OR
                       v_tx_type NOT IN ('EXPENSE', 'INCOME') OR v_tx_date IS NULL THEN
                        v_results := v_results || jsonb_build_object(
                            'mutationId', v_mutation_id,
                            'status', 'REJECTED',
                            'message', 'Invalid transaction payload data'
                        );
                        CONTINUE;
                    END IF;

                    INSERT INTO public.transactions (
                        id, user_id, description, amount, type, category,
                        transaction_date, device_id, mutation_id, updated_at_epoch_millis, deleted_at
                    ) VALUES (
                        v_tx_id, v_user_id, v_tx_desc, v_tx_amount, v_tx_type, v_tx_category,
                        v_tx_date, v_device_id, v_mutation_id, v_updated_at_epoch, NULL
                    ) ON CONFLICT (id) DO UPDATE SET
                        description = EXCLUDED.description,
                        amount = EXCLUDED.amount,
                        type = EXCLUDED.type,
                        category = EXCLUDED.category,
                        transaction_date = EXCLUDED.transaction_date,
                        device_id = EXCLUDED.device_id,
                        mutation_id = EXCLUDED.mutation_id,
                        updated_at_epoch_millis = EXCLUDED.updated_at_epoch_millis,
                        deleted_at = NULL;

                    v_results := v_results || jsonb_build_object(
                        'mutationId', v_mutation_id,
                        'status', 'APPLIED'
                    );

                ELSIF v_operation = 'DELETE' THEN
                    IF FOUND THEN
                        UPDATE public.transactions
                        SET deleted_at = timezone('utc'::text, to_timestamp(v_updated_at_epoch / 1000.0)),
                            device_id = v_device_id,
                            mutation_id = v_mutation_id,
                            updated_at_epoch_millis = v_updated_at_epoch
                        WHERE id = v_tx_id;
                    ELSE
                        -- Tombstone insert for deleted item that was never uploaded
                        v_tx_desc := COALESCE(NULLIF(trim(v_payload->>'description'), ''), 'Deleted transaction');
                        v_tx_amount := COALESCE((v_payload->>'amount')::BIGINT, 1);
                        v_tx_type := COALESCE(v_payload->>'type', 'EXPENSE');
                        v_tx_category := COALESCE(v_payload->>'category', 'OTHER');
                        v_tx_date := timezone('utc'::text, to_timestamp(v_updated_at_epoch / 1000.0));

                        INSERT INTO public.transactions (
                            id, user_id, description, amount, type, category,
                            transaction_date, device_id, mutation_id, updated_at_epoch_millis, deleted_at
                        ) VALUES (
                            v_tx_id, v_user_id, v_tx_desc, v_tx_amount, v_tx_type, v_tx_category,
                            v_tx_date, v_device_id, v_mutation_id, v_updated_at_epoch,
                            timezone('utc'::text, to_timestamp(v_updated_at_epoch / 1000.0))
                        ) ON CONFLICT (id) DO UPDATE SET
                            deleted_at = EXCLUDED.deleted_at,
                            device_id = EXCLUDED.device_id,
                            mutation_id = EXCLUDED.mutation_id,
                            updated_at_epoch_millis = EXCLUDED.updated_at_epoch_millis;
                    END IF;

                    v_results := v_results || jsonb_build_object(
                        'mutationId', v_mutation_id,
                        'status', 'APPLIED'
                    );
                END IF;

            WHEN 'MONTHLY_BUDGET' THEN
                v_mb_year := (v_payload->>'year')::INT;
                v_mb_month := (v_payload->>'month')::INT;
                IF v_mb_year IS NULL OR v_mb_month IS NULL THEN
                    v_results := v_results || jsonb_build_object(
                        'mutationId', v_mutation_id,
                        'status', 'REJECTED',
                        'message', 'Missing year or month'
                    );
                    CONTINUE;
                END IF;

                SELECT * INTO v_existing_mb
                FROM public.monthly_budgets
                WHERE user_id = v_user_id AND year = v_mb_year AND month = v_mb_month;

                IF FOUND THEN
                    IF v_existing_mb.mutation_id = v_mutation_id THEN
                        v_results := v_results || jsonb_build_object(
                            'mutationId', v_mutation_id,
                            'status', 'APPLIED',
                            'message', 'Already applied'
                        );
                        CONTINUE;
                    END IF;

                    IF NOT public.is_mutation_winner(
                        v_updated_at_epoch, v_device_id, v_mutation_id,
                        v_existing_mb.updated_at_epoch_millis, v_existing_mb.device_id, v_existing_mb.mutation_id
                    ) THEN
                        v_results := v_results || jsonb_build_object(
                            'mutationId', v_mutation_id,
                            'status', 'STALE',
                            'message', 'Incoming mutation superseded by newer remote state'
                        );
                        CONTINUE;
                    END IF;
                END IF;

                IF v_operation = 'UPSERT' THEN
                    v_mb_limit := (v_payload->>'limitAmount')::BIGINT;
                    IF v_mb_limit IS NULL OR v_mb_limit <= 0 THEN
                        v_results := v_results || jsonb_build_object(
                            'mutationId', v_mutation_id,
                            'status', 'REJECTED',
                            'message', 'Invalid budget limit'
                        );
                        CONTINUE;
                    END IF;

                    INSERT INTO public.monthly_budgets (
                        user_id, year, month, limit_amount,
                        device_id, mutation_id, updated_at_epoch_millis, deleted_at
                    ) VALUES (
                        v_user_id, v_mb_year, v_mb_month, v_mb_limit,
                        v_device_id, v_mutation_id, v_updated_at_epoch, NULL
                    ) ON CONFLICT (user_id, year, month) DO UPDATE SET
                        limit_amount = EXCLUDED.limit_amount,
                        device_id = EXCLUDED.device_id,
                        mutation_id = EXCLUDED.mutation_id,
                        updated_at_epoch_millis = EXCLUDED.updated_at_epoch_millis,
                        deleted_at = NULL;

                    v_results := v_results || jsonb_build_object(
                        'mutationId', v_mutation_id,
                        'status', 'APPLIED'
                    );

                ELSIF v_operation = 'DELETE' THEN
                    IF FOUND THEN
                        UPDATE public.monthly_budgets
                        SET deleted_at = timezone('utc'::text, to_timestamp(v_updated_at_epoch / 1000.0)),
                            device_id = v_device_id,
                            mutation_id = v_mutation_id,
                            updated_at_epoch_millis = v_updated_at_epoch
                        WHERE user_id = v_user_id AND year = v_mb_year AND month = v_mb_month;
                    ELSE
                        v_mb_limit := COALESCE((v_payload->>'limitAmount')::BIGINT, 1);
                        INSERT INTO public.monthly_budgets (
                            user_id, year, month, limit_amount,
                            device_id, mutation_id, updated_at_epoch_millis, deleted_at
                        ) VALUES (
                            v_user_id, v_mb_year, v_mb_month, v_mb_limit,
                            v_device_id, v_mutation_id, v_updated_at_epoch,
                            timezone('utc'::text, to_timestamp(v_updated_at_epoch / 1000.0))
                        ) ON CONFLICT (user_id, year, month) DO UPDATE SET
                            deleted_at = EXCLUDED.deleted_at,
                            device_id = EXCLUDED.device_id,
                            mutation_id = EXCLUDED.mutation_id,
                            updated_at_epoch_millis = EXCLUDED.updated_at_epoch_millis;
                    END IF;

                    v_results := v_results || jsonb_build_object(
                        'mutationId', v_mutation_id,
                        'status', 'APPLIED'
                    );
                END IF;

            WHEN 'CATEGORY_BUDGET' THEN
                v_cb_year := (v_payload->>'year')::INT;
                v_cb_month := (v_payload->>'month')::INT;
                v_cb_category := v_payload->>'category';
                IF v_cb_year IS NULL OR v_cb_month IS NULL OR v_cb_category IS NULL THEN
                    v_results := v_results || jsonb_build_object(
                        'mutationId', v_mutation_id,
                        'status', 'REJECTED',
                        'message', 'Missing category budget key fields'
                    );
                    CONTINUE;
                END IF;

                SELECT * INTO v_existing_cb
                FROM public.category_budgets
                WHERE user_id = v_user_id AND year = v_cb_year AND month = v_cb_month AND category = v_cb_category;

                IF FOUND THEN
                    IF v_existing_cb.mutation_id = v_mutation_id THEN
                        v_results := v_results || jsonb_build_object(
                            'mutationId', v_mutation_id,
                            'status', 'APPLIED',
                            'message', 'Already applied'
                        );
                        CONTINUE;
                    END IF;

                    IF NOT public.is_mutation_winner(
                        v_updated_at_epoch, v_device_id, v_mutation_id,
                        v_existing_cb.updated_at_epoch_millis, v_existing_cb.device_id, v_existing_cb.mutation_id
                    ) THEN
                        v_results := v_results || jsonb_build_object(
                            'mutationId', v_mutation_id,
                            'status', 'STALE',
                            'message', 'Incoming mutation superseded by newer remote state'
                        );
                        CONTINUE;
                    END IF;
                END IF;

                IF v_operation = 'UPSERT' THEN
                    v_cb_limit := (v_payload->>'limitAmount')::BIGINT;
                    IF v_cb_limit IS NULL OR v_cb_limit <= 0 THEN
                        v_results := v_results || jsonb_build_object(
                            'mutationId', v_mutation_id,
                            'status', 'REJECTED',
                            'message', 'Invalid category budget limit'
                        );
                        CONTINUE;
                    END IF;

                    INSERT INTO public.category_budgets (
                        user_id, year, month, category, limit_amount,
                        device_id, mutation_id, updated_at_epoch_millis, deleted_at
                    ) VALUES (
                        v_user_id, v_cb_year, v_cb_month, v_cb_category, v_cb_limit,
                        v_device_id, v_mutation_id, v_updated_at_epoch, NULL
                    ) ON CONFLICT (user_id, year, month, category) DO UPDATE SET
                        limit_amount = EXCLUDED.limit_amount,
                        device_id = EXCLUDED.device_id,
                        mutation_id = EXCLUDED.mutation_id,
                        updated_at_epoch_millis = EXCLUDED.updated_at_epoch_millis,
                        deleted_at = NULL;

                    v_results := v_results || jsonb_build_object(
                        'mutationId', v_mutation_id,
                        'status', 'APPLIED'
                    );

                ELSIF v_operation = 'DELETE' THEN
                    IF FOUND THEN
                        UPDATE public.category_budgets
                        SET deleted_at = timezone('utc'::text, to_timestamp(v_updated_at_epoch / 1000.0)),
                            device_id = v_device_id,
                            mutation_id = v_mutation_id,
                            updated_at_epoch_millis = v_updated_at_epoch
                        WHERE user_id = v_user_id AND year = v_cb_year AND month = v_cb_month AND category = v_cb_category;
                    ELSE
                        v_cb_limit := COALESCE((v_payload->>'limitAmount')::BIGINT, 1);
                        INSERT INTO public.category_budgets (
                            user_id, year, month, category, limit_amount,
                            device_id, mutation_id, updated_at_epoch_millis, deleted_at
                        ) VALUES (
                            v_user_id, v_cb_year, v_cb_month, v_cb_category, v_cb_limit,
                            v_device_id, v_mutation_id, v_updated_at_epoch,
                            timezone('utc'::text, to_timestamp(v_updated_at_epoch / 1000.0))
                        ) ON CONFLICT (user_id, year, month, category) DO UPDATE SET
                            deleted_at = EXCLUDED.deleted_at,
                            device_id = EXCLUDED.device_id,
                            mutation_id = EXCLUDED.mutation_id,
                            updated_at_epoch_millis = EXCLUDED.updated_at_epoch_millis;
                    END IF;

                    v_results := v_results || jsonb_build_object(
                        'mutationId', v_mutation_id,
                        'status', 'APPLIED'
                    );
                END IF;

            ELSE
                v_results := v_results || jsonb_build_object(
                    'mutationId', v_mutation_id,
                    'status', 'REJECTED',
                    'message', 'Unknown entityType'
                );
        END CASE;
    END LOOP;

    RETURN v_results;
END;
$$;

-- 6. RPC: pull_sync_changes
CREATE OR REPLACE FUNCTION public.pull_sync_changes(p_since_epoch_millis BIGINT DEFAULT 0)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
    v_user_id UUID;
    v_server_now BIGINT;
    v_tx JSONB;
    v_mb JSONB;
    v_cb JSONB;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthenticated request' USING ERRCODE = '42501';
    END IF;

    v_server_now := (EXTRACT(EPOCH FROM timezone('utc'::text, now())) * 1000)::BIGINT;

    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'id', id,
        'userId', user_id,
        'description', description,
        'amount', amount,
        'type', type,
        'category', category,
        'dateEpochMillis', (EXTRACT(EPOCH FROM transaction_date) * 1000)::BIGINT,
        'updatedAtEpochMillis', updated_at_epoch_millis,
        'deletedAtEpochMillis', CASE WHEN deleted_at IS NOT NULL THEN (EXTRACT(EPOCH FROM deleted_at) * 1000)::BIGINT ELSE NULL END,
        'deviceId', device_id,
        'mutationId', mutation_id
    ) ORDER BY updated_at_epoch_millis ASC), '[]'::jsonb)
    INTO v_tx
    FROM public.transactions
    WHERE user_id = v_user_id AND updated_at_epoch_millis > COALESCE(p_since_epoch_millis, 0);

    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'userId', user_id,
        'year', year,
        'month', month,
        'limitAmount', limit_amount,
        'updatedAtEpochMillis', updated_at_epoch_millis,
        'deletedAtEpochMillis', CASE WHEN deleted_at IS NOT NULL THEN (EXTRACT(EPOCH FROM deleted_at) * 1000)::BIGINT ELSE NULL END,
        'deviceId', device_id,
        'mutationId', mutation_id
    ) ORDER BY updated_at_epoch_millis ASC), '[]'::jsonb)
    INTO v_mb
    FROM public.monthly_budgets
    WHERE user_id = v_user_id AND updated_at_epoch_millis > COALESCE(p_since_epoch_millis, 0);

    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'userId', user_id,
        'year', year,
        'month', month,
        'category', category,
        'limitAmount', limit_amount,
        'updatedAtEpochMillis', updated_at_epoch_millis,
        'deletedAtEpochMillis', CASE WHEN deleted_at IS NOT NULL THEN (EXTRACT(EPOCH FROM deleted_at) * 1000)::BIGINT ELSE NULL END,
        'deviceId', device_id,
        'mutationId', mutation_id
    ) ORDER BY updated_at_epoch_millis ASC), '[]'::jsonb)
    INTO v_cb
    FROM public.category_budgets
    WHERE user_id = v_user_id AND updated_at_epoch_millis > COALESCE(p_since_epoch_millis, 0);

    RETURN jsonb_build_object(
        'transactions', v_tx,
        'monthlyBudgets', v_mb,
        'categoryBudgets', v_cb,
        'serverEpochMillis', v_server_now
    );
END;
$$;

-- 7. Grants
REVOKE ALL ON FUNCTION public.is_mutation_winner FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.apply_sync_mutations FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.pull_sync_changes FROM PUBLIC, anon;

GRANT EXECUTE ON FUNCTION public.is_mutation_winner TO authenticated;
GRANT EXECUTE ON FUNCTION public.apply_sync_mutations TO authenticated;
GRANT EXECUTE ON FUNCTION public.pull_sync_changes TO authenticated;
