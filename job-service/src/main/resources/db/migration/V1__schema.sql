CREATE TABLE scraping_jobs (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    phase VARCHAR(32) NOT NULL,
    category_ids TEXT NOT NULL,
    country_codes TEXT NOT NULL DEFAULT '[]',
    city_ids TEXT NOT NULL DEFAULT '[]',
    enabled_providers TEXT NOT NULL DEFAULT '[]',
    max_companies INT NOT NULL DEFAULT 200,
    discovered_count INT NOT NULL DEFAULT 0,
    enriched_count INT NOT NULL DEFAULT 0,
    persisted_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    progress_percent INT NOT NULL DEFAULT 0,
    estimated_remaining_seconds BIGINT,
    export_id VARCHAR(128),
    error_message TEXT,
    checkpoint TEXT,
    correlation_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_scraping_jobs_status ON scraping_jobs (status);
CREATE INDEX idx_scraping_jobs_user_id ON scraping_jobs (user_id);
CREATE INDEX idx_scraping_jobs_created_at ON scraping_jobs (created_at DESC);
CREATE INDEX idx_scraping_jobs_user_created ON scraping_jobs (user_id, created_at DESC);
CREATE INDEX idx_scraping_jobs_status_created ON scraping_jobs (status, created_at DESC);

CREATE TABLE scraping_job_progress (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES scraping_jobs (id) ON DELETE CASCADE,
    phase VARCHAR(32) NOT NULL,
    message TEXT,
    counts TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_scraping_job_progress_job_id ON scraping_job_progress (job_id);
CREATE INDEX idx_scraping_job_progress_job_created ON scraping_job_progress (job_id, created_at DESC);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES scraping_jobs (id) ON DELETE CASCADE,
    user_id VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_job_id ON audit_logs (job_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_job_created ON audit_logs (job_id, created_at DESC);
