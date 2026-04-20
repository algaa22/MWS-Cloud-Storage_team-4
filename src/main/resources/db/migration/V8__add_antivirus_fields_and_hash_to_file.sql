ALTER TABLE files
    ADD COLUMN IF NOT EXISTS scan_verdict VARCHAR(30) DEFAULT 'UNKNOWN',
    ADD COLUMN IF NOT EXISTS scan_level VARCHAR(20) DEFAULT 'NONE';
    ADD COLUMN IF NOT EXISTS hash CHAR(64);

ALTER TABLE chunked_upload_sessions
    ADD COLUMN IF NOT EXISTS hash CHAR(64);

CREATE INDEX IF NOT EXISTS idx_files_scan_verdict_infected
    ON files (scan_verdict)
    WHERE scan_verdict = 'INFECTED';