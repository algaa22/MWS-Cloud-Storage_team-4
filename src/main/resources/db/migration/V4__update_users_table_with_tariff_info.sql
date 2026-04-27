ALTER TABLE users
    ADD COLUMN IF NOT EXISTS free_storage_limit BIGINT DEFAULT 5368709120;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS user_status VARCHAR (20) DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS scheduled_deletion_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS trial_start_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS trial_end_date TIMESTAMP;

UPDATE users
SET free_storage_limit = 5368709120
WHERE free_storage_limit IS NULL;
UPDATE users
SET user_status = 'ACTIVE'
WHERE user_status IS NULL;

CREATE INDEX IF NOT EXISTS idx_users_scheduled_deletion
    ON users (scheduled_deletion_date)
    WHERE scheduled_deletion_date IS NOT NULL;