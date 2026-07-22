CREATE TABLE companies (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    website VARCHAR(512) NOT NULL,
    industry VARCHAR(100) NOT NULL,
    city_id VARCHAR(64) NOT NULL,
    country_code VARCHAR(2) NOT NULL
);

CREATE TABLE company_categories (
    company_id VARCHAR(64) NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    category_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (company_id, category_id)
);

CREATE INDEX idx_companies_city_id ON companies (city_id);
CREATE INDEX idx_companies_name_lower ON companies (LOWER(name));
