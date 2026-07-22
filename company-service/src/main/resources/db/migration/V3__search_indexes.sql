CREATE INDEX IF NOT EXISTS idx_companies_city_id ON companies (city_id);
CREATE INDEX IF NOT EXISTS idx_companies_name ON companies (name);
CREATE INDEX IF NOT EXISTS idx_companies_industry ON companies (industry);
CREATE INDEX IF NOT EXISTS idx_company_categories_category_id ON company_categories (category_id);
CREATE INDEX IF NOT EXISTS idx_company_categories_company_id ON company_categories (company_id);
