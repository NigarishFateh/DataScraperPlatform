-- Remove profiles produced from previous seeded-catalog enrichment runs.
TRUNCATE TABLE
    company_sources,
    company_socials,
    company_technologies,
    company_locations,
    company_contacts,
    company_profile_categories,
    normalization_logs,
    company_profiles
CASCADE;
