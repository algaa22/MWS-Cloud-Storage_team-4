ALTER TABLE files ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_files_deleted_at ON files (deleted_at)
WHERE is_deleted = TRUE AND deleted_at IS NOT NULL;