-- Global BI Platform persistence (enriched company profiles per scraping job)

CREATE TABLE company_profiles (
    id VARCHAR(64) PRIMARY KEY,
    company_id VARCHAR(64) REFERENCES companies (id) ON DELETE SET NULL,
    job_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    industry VARCHAR(255),
    country_code VARCHAR(8),
    country_name VARCHAR(255),
    state VARCHAR(255),
    city VARCHAR(255),
    website VARCHAR(512),
    description TEXT,
    services TEXT,
    products TEXT,
    founder VARCHAR(255),
    ceo VARCHAR(255),
    founded_year INT,
    employee_count VARCHAR(64),
    address TEXT,
    contact_page VARCHAR(512),
    source_url VARCHAR(512),
    confidence_score DOUBLE PRECISION,
    provider_name VARCHAR(128),
    notes TEXT,
    scraped_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    normalized_key VARCHAR(512) NOT NULL
);

CREATE TABLE company_contacts (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL REFERENCES company_profiles (id) ON DELETE CASCADE,
    email VARCHAR(255),
    phone VARCHAR(64),
    contact_page VARCHAR(512)
);

CREATE TABLE company_locations (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL REFERENCES company_profiles (id) ON DELETE CASCADE,
    country_code VARCHAR(8),
    state VARCHAR(255),
    city VARCHAR(255),
    address TEXT
);

CREATE TABLE company_technologies (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL REFERENCES company_profiles (id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE company_profile_categories (
    profile_id VARCHAR(64) NOT NULL REFERENCES company_profiles (id) ON DELETE CASCADE,
    category_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (profile_id, category_id)
);

CREATE TABLE company_socials (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL REFERENCES company_profiles (id) ON DELETE CASCADE,
    platform VARCHAR(64) NOT NULL,
    url VARCHAR(512) NOT NULL
);

CREATE TABLE company_sources (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL REFERENCES company_profiles (id) ON DELETE CASCADE,
    provider_name VARCHAR(128),
    source_url VARCHAR(512),
    raw_json TEXT,
    scraped_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE normalization_logs (
    id VARCHAR(64) PRIMARY KEY,
    job_id UUID NOT NULL,
    profile_id VARCHAR(64) REFERENCES company_profiles (id) ON DELETE SET NULL,
    action VARCHAR(64) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE validation_logs (
    id VARCHAR(64) PRIMARY KEY,
    job_id UUID NOT NULL,
    profile_id VARCHAR(64) REFERENCES company_profiles (id) ON DELETE SET NULL,
    valid BOOLEAN NOT NULL,
    issues TEXT,
    confidence DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE provider_executions (
    id VARCHAR(64) PRIMARY KEY,
    job_id UUID NOT NULL,
    company_name VARCHAR(255),
    provider_type VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    message TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_company_profiles_job_normalized_key ON company_profiles (job_id, normalized_key);
CREATE INDEX idx_company_profiles_job_id ON company_profiles (job_id);
CREATE INDEX idx_company_profiles_normalized_key ON company_profiles (normalized_key);
CREATE INDEX idx_company_profiles_website ON company_profiles (website);
CREATE INDEX idx_company_profiles_country_code ON company_profiles (country_code);
CREATE INDEX idx_company_profiles_confidence_score ON company_profiles (confidence_score);
CREATE INDEX idx_company_profiles_job_name ON company_profiles (job_id, name);

CREATE INDEX idx_company_contacts_profile_id ON company_contacts (profile_id);
CREATE INDEX idx_company_locations_profile_id ON company_locations (profile_id);
CREATE INDEX idx_company_technologies_profile_id ON company_technologies (profile_id);
CREATE INDEX idx_company_profile_categories_category_id ON company_profile_categories (category_id);
CREATE INDEX idx_company_socials_profile_id ON company_socials (profile_id);
CREATE INDEX idx_company_sources_profile_id ON company_sources (profile_id);
CREATE INDEX idx_normalization_logs_job_id ON normalization_logs (job_id);
CREATE INDEX idx_validation_logs_job_id ON validation_logs (job_id);
CREATE INDEX idx_provider_executions_job_id ON provider_executions (job_id);
