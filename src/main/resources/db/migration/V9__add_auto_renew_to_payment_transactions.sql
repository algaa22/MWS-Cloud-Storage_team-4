ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS auto_renew BOOLEAN DEFAULT FALSE;

ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS storage_limit_gb BIGINT,
    ADD COLUMN IF NOT EXISTS price DECIMAL (10, 2),
    ADD COLUMN IF NOT EXISTS duration_days INTEGER;