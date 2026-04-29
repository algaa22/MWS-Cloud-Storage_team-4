ALTER TABLE users
    ADD COLUMN IF NOT EXISTS auto_renew BOOLEAN DEFAULT true;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS tariff_plan VARCHAR (20),
    ADD COLUMN IF NOT EXISTS tariff_start_date TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS tariff_end_date TIMESTAMP WITH TIME ZONE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS payment_method_id VARCHAR (255);

DO
$$
BEGIN
    IF
EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='users' AND column_name='storage_limit'
    ) THEN
ALTER TABLE users RENAME COLUMN storage_limit TO paid_storage_limit;
END IF;
END $$;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS paid_storage_limit BIGINT;

UPDATE users
SET auto_renew = true
WHERE auto_renew IS NULL;