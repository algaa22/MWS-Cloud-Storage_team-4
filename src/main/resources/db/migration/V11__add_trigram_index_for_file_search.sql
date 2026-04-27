CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_storage_entities_name_trgm
    ON files USING gin (name gin_trgm_ops);

SET pg_trgm.similarity_threshold = 0.4;