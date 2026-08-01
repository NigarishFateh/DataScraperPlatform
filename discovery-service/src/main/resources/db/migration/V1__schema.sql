CREATE TABLE discovery_logs (
    id              UUID PRIMARY KEY,
    job_id          UUID NOT NULL,
    provider_name   VARCHAR(128) NOT NULL,
    provider_type   VARCHAR(64) NOT NULL,
    request_summary TEXT,
    result_count    INT NOT NULL DEFAULT 0,
    status          VARCHAR(32) NOT NULL,
    message         TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_discovery_logs_job_id ON discovery_logs (job_id);
CREATE INDEX idx_discovery_logs_provider_type ON discovery_logs (provider_type);
