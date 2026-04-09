DROP INDEX IF EXISTS idx_files_upsert;

CREATE UNIQUE INDEX idx_files_upsert_active
    ON files (user_id, name, ( COALESCE (parent_id,
              '00000000-0000-0000-0000-000000000000'))) WHERE is_deleted = FALSE;