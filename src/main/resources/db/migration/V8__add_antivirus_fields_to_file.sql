ALTER TABLE files
    ADD COLUMN IF NOT EXISTS scan_result VARCHAR(30) DEFAULT 'UNKNOWN',
    ADD COLUMN IF NOT EXISTS scan_level VARCHAR(20) DEFAULT 'NONE';

CREATE INDEX IF NOT EXISTS idx_files_scan_result_infected
    ON files (scan_result)
    WHERE scan_result = 'INFECTED';