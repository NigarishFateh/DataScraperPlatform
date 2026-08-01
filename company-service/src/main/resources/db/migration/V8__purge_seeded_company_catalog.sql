-- Remove all seeded / catalog demo companies. Real company rows are written
-- only by the enrichment persistence pipeline (company_profiles + optional upsert).
DELETE FROM company_categories;
DELETE FROM companies;
