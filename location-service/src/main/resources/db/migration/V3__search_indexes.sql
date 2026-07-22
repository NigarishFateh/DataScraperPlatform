CREATE INDEX IF NOT EXISTS idx_cities_country_code ON cities (country_code);
CREATE INDEX IF NOT EXISTS idx_cities_country_name ON cities (country_code, name);
