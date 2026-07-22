CREATE TABLE countries (
    code VARCHAR(2) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE cities (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country_code VARCHAR(2) NOT NULL REFERENCES countries (code)
);

CREATE INDEX idx_cities_country_code ON cities (country_code);
