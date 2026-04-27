DROP TABLE IF EXISTS share_recipients CASCADE;
DROP TABLE IF EXISTS file_shares CASCADE;

CREATE TABLE file_shares
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id        UUID               NOT NULL,
    share_token    VARCHAR(36) UNIQUE NOT NULL,
    created_by     UUID               NOT NULL,
    created_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    expires_at     TIMESTAMP,
    password_hash  VARCHAR(255),
    max_downloads  INTEGER,
    download_count INTEGER          DEFAULT 0,
    is_active      BOOLEAN          DEFAULT true,
    share_type     VARCHAR(20)      DEFAULT 'PUBLIC',

    CONSTRAINT fk_file_shares_file FOREIGN KEY (file_id)
        REFERENCES files (id) ON DELETE CASCADE,
    CONSTRAINT fk_file_shares_user FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT check_share_type CHECK (
        share_type IN ('PUBLIC', 'PROTECTED')
        )
);