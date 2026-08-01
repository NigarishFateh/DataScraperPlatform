CREATE INDEX IF NOT EXISTS idx_categories_name ON categories (name);

INSERT INTO categories (id, name)
SELECT v.id, v.name
FROM (VALUES
  ('cleaning', 'Cleaning Companies'),
  ('software', 'Software'),
  ('it', 'Information Technology'),
  ('cloud', 'Cloud Computing'),
  ('ai', 'Artificial Intelligence'),
  ('cybersecurity', 'Cybersecurity'),
  ('construction', 'Construction'),
  ('retail', 'Retail'),
  ('healthcare', 'Healthcare'),
  ('education', 'Education'),
  ('hospitality', 'Hospitality'),
  ('finance', 'Finance'),
  ('agriculture', 'Agriculture'),
  ('manufacturing', 'Manufacturing'),
  ('telecommunications', 'Telecommunications'),
  ('logistics', 'Logistics'),
  ('recruitment', 'Recruitment'),
  ('consulting', 'Consulting'),
  ('real-estate', 'Real Estate'),
  ('legal', 'Legal'),
  ('energy', 'Energy'),
  ('food', 'Food & Beverage'),
  ('automotive', 'Automotive'),
  ('government', 'Government'),
  ('non-profit', 'Non-Profit')
) AS v(id, name)
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.id = v.id);
