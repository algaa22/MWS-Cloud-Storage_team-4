CREATE TABLE IF NOT EXISTS chunked_upload_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    upload_id TEXT NOT NULL,
    total_parts INT NOT NULL,
    current_size BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING'
);

CREATE TABLE IF NOT EXISTS chunked_upload_parts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES chunked_upload_sessions(id) ON DELETE CASCADE,
    part_number INT NOT NULL,
    part_size BIGINT NOT NULL,
    etag TEXT NOT NULL,

    UNIQUE (session_id, part_number)
);

CREATE INDEX IF NOT EXISTS idx_chunk_parts_session ON chunked_upload_parts(session_id, part_number);