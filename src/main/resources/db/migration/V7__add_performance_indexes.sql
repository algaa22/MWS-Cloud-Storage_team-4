CREATE INDEX IF NOT EXISTS idx_files_user_parent ON files(user_id, parent_id);

CREATE INDEX IF NOT EXISTS idx_files_parent_id ON files(parent_id);

CREATE INDEX IF NOT EXISTS idx_files_stale ON files(updated_at) WHERE status != 'READY';