CREATE
EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    email VARCHAR
(
    255
) UNIQUE NOT NULL,
    password_hash VARCHAR
(
    255
) NOT NULL,
    username VARCHAR
(
    100
) NOT NULL,
    storage_limit BIGINT DEFAULT 10737418240,
    used_storage BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             is_active BOOLEAN DEFAULT true
                             );

CREATE TABLE IF NOT EXISTS files
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    user_id UUID NOT NULL REFERENCES users
(
    id
) ON DELETE CASCADE,
    parent_id UUID REFERENCES files
(
    id
)
  ON DELETE CASCADE,
    name VARCHAR
(
    500
) NOT NULL,
    size BIGINT NOT NULL,
    mime_type VARCHAR
(
    100
),
    tags VARCHAR
(
    500
),
    visibility VARCHAR
(
    20
) DEFAULT 'private',
    is_deleted BOOLEAN DEFAULT false,
    is_directory BOOLEAN DEFAULT false,

    status VARCHAR
(
    20
) NOT NULL DEFAULT 'READY',
    operation_type VARCHAR
(
    30
),
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
  WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
      retry_count INT DEFAULT 0,
      error_message TEXT,
      CONSTRAINT check_no_self_reference CHECK (id != parent_id)
    );

CREATE UNIQUE INDEX IF NOT EXISTS idx_files_upsert
    ON files (user_id, name, (COALESCE (parent_id, '00000000-0000-0000-0000-000000000000')));

CREATE TABLE IF NOT EXISTS refresh_tokens
(
    id
    UUID
    PRIMARY
    KEY,
    user_id
    UUID
    NOT
    NULL
    REFERENCES
    users
(
    id
) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP
  WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );