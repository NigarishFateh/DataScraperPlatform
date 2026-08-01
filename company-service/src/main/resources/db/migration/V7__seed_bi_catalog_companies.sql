-- Seed global catalog companies across BI categories (including default: cleaning).
-- Discovery providers query this catalog; enrichment providers fill public fields only.

INSERT INTO companies (id, name, website, industry, city_id, country_code)
SELECT * FROM (VALUES
  ('clean-us-01', 'ServiceMaster Clean', 'https://www.servicemasterclean.com', 'Cleaning', 'us-chicago', 'US'),
  ('clean-us-02', 'Merry Maids', 'https://www.merrymaids.com', 'Cleaning', 'us-dallas', 'US'),
  ('clean-gb-01', 'Cleanology', 'https://www.cleanology.com', 'Cleaning', 'gb-london', 'GB'),
  ('clean-in-01', 'BVG India', 'https://www.bvgindia.com', 'Cleaning', 'in-mumbai', 'IN'),
  ('clean-de-01', 'Counterserve', 'https://www.counterserve.de', 'Cleaning', 'de-berlin', 'DE'),
  ('soft-us-01', 'Microsoft', 'https://www.microsoft.com', 'Software', 'us-seattle', 'US'),
  ('soft-in-01', 'Infosys', 'https://www.infosys.com', 'Software', 'in-bengaluru', 'IN'),
  ('cloud-us-01', 'Amazon Web Services', 'https://aws.amazon.com', 'Cloud', 'us-seattle', 'US'),
  ('ai-us-01', 'OpenAI', 'https://openai.com', 'Artificial Intelligence', 'us-san-francisco', 'US'),
  ('cyber-us-01', 'CrowdStrike', 'https://www.crowdstrike.com', 'Cybersecurity', 'us-austin', 'US'),
  ('const-us-01', 'Turner Construction', 'https://www.turnerconstruction.com', 'Construction', 'us-new-york', 'US'),
  ('retail-us-01', 'Walmart', 'https://www.walmart.com', 'Retail', 'us-dallas', 'US'),
  ('health-us-01', 'Mayo Clinic', 'https://www.mayoclinic.org', 'Healthcare', 'us-chicago', 'US'),
  ('edu-us-01', 'Coursera', 'https://www.coursera.org', 'Education', 'us-san-francisco', 'US'),
  ('hosp-us-01', 'Marriott International', 'https://www.marriott.com', 'Hospitality', 'us-washington-dc', 'US'),
  ('fin-us-01', 'JPMorgan Chase', 'https://www.jpmorganchase.com', 'Finance', 'us-new-york', 'US'),
  ('agri-nl-01', 'Bayer Crop Science', 'https://www.bayer.com', 'Agriculture', 'nl-amsterdam', 'NL'),
  ('mfg-de-01', 'Siemens', 'https://www.siemens.com', 'Manufacturing', 'de-munich', 'DE'),
  ('telco-us-01', 'Verizon', 'https://www.verizon.com', 'Telecommunications', 'us-new-york', 'US'),
  ('log-us-01', 'FedEx', 'https://www.fedex.com', 'Logistics', 'us-houston', 'US'),
  ('rec-us-01', 'Robert Half', 'https://www.roberthalf.com', 'Recruitment', 'us-san-francisco', 'US'),
  ('cons-us-01', 'McKinsey & Company', 'https://www.mckinsey.com', 'Consulting', 'us-new-york', 'US'),
  ('re-us-01', 'CBRE', 'https://www.cbre.com', 'Real Estate', 'us-dallas', 'US'),
  ('legal-us-01', 'Baker McKenzie', 'https://www.bakermckenzie.com', 'Legal', 'us-chicago', 'US'),
  ('energy-us-01', 'NextEra Energy', 'https://www.nexteraenergy.com', 'Energy', 'us-miami', 'US'),
  ('food-us-01', 'Nestlé USA', 'https://www.nestleusa.com', 'Food', 'us-los-angeles', 'US'),
  ('auto-de-01', 'BMW Group', 'https://www.bmwgroup.com', 'Automotive', 'de-munich', 'DE'),
  ('gov-us-01', 'US Small Business Administration', 'https://www.sba.gov', 'Government', 'us-washington-dc', 'US'),
  ('npos-us-01', 'Red Cross', 'https://www.redcross.org', 'Non-profit', 'us-washington-dc', 'US')
) AS v(id, name, website, industry, city_id, country_code)
WHERE NOT EXISTS (SELECT 1 FROM companies c WHERE c.id = v.id);

-- Map to BI category ids (category-service V3)
INSERT INTO company_categories (company_id, category_id)
SELECT * FROM (VALUES
  ('clean-us-01', 'cleaning'),
  ('clean-us-02', 'cleaning'),
  ('clean-gb-01', 'cleaning'),
  ('clean-in-01', 'cleaning'),
  ('clean-de-01', 'cleaning'),
  ('soft-us-01', 'software'),
  ('soft-in-01', 'software'),
  ('cloud-us-01', 'cloud'),
  ('ai-us-01', 'ai'),
  ('cyber-us-01', 'cybersecurity'),
  ('const-us-01', 'construction'),
  ('retail-us-01', 'retail'),
  ('health-us-01', 'healthcare'),
  ('edu-us-01', 'education'),
  ('hosp-us-01', 'hospitality'),
  ('fin-us-01', 'finance'),
  ('agri-nl-01', 'agriculture'),
  ('mfg-de-01', 'manufacturing'),
  ('telco-us-01', 'telecommunications'),
  ('log-us-01', 'logistics'),
  ('rec-us-01', 'recruitment'),
  ('cons-us-01', 'consulting'),
  ('re-us-01', 'real-estate'),
  ('legal-us-01', 'legal'),
  ('energy-us-01', 'energy'),
  ('food-us-01', 'food'),
  ('auto-de-01', 'automotive'),
  ('gov-us-01', 'government'),
  ('npos-us-01', 'non-profit')
) AS v(company_id, category_id)
WHERE NOT EXISTS (
  SELECT 1 FROM company_categories cc
  WHERE cc.company_id = v.company_id AND cc.category_id = v.category_id
);
