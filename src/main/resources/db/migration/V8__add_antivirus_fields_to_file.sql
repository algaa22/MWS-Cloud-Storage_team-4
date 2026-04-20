ALTER TABLE files
    ADD COLUMN IF NOT EXISTS scan_verdict VARCHAR(30) DEFAULT 'UNKNOWN',
    ADD COLUMN IF NOT EXISTS scan_level VARCHAR(20) DEFAULT 'NONE';

CREATE INDEX IF NOT EXISTS idx_files_scan_verdict_infected
    ON files (scan_verdict)
    WHERE scan_verdict = 'INFECTED';