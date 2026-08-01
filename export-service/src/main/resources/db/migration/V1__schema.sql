CREATE TABLE export_history (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    format VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    file_name VARCHAR(512),
    file_path VARCHAR(1024),
    row_count BIGINT,
    file_size_bytes BIGINT,
    error_message TEXT,
    search_criteria TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_export_history_job_id ON export_history (job_id);
CREATE INDEX idx_export_history_status ON export_history (status);
CREATE INDEX idx_export_history_created_at ON export_history (created_at);
