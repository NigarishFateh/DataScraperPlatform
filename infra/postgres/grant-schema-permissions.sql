-- Quick fix for PostgreSQL 15+ schema permissions (run as postgres superuser)
--   "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f infra/postgres/grant-schema-permissions.sql

ALTER DATABASE auth_db OWNER TO datascraper;
ALTER DATABASE location_db OWNER TO datascraper;
ALTER DATABASE company_db OWNER TO datascraper;
ALTER DATABASE category_db OWNER TO datascraper;

\c auth_db
ALTER SCHEMA public OWNER TO datascraper;
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
GRANT ALL ON ALL TABLES IN SCHEMA public TO datascraper;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO datascraper;

\c location_db
ALTER SCHEMA public OWNER TO datascraper;
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
GRANT ALL ON ALL TABLES IN SCHEMA public TO datascraper;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO datascraper;

\c company_db
ALTER SCHEMA public OWNER TO datascraper;
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
GRANT ALL ON ALL TABLES IN SCHEMA public TO datascraper;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO datascraper;

\c category_db
ALTER SCHEMA public OWNER TO datascraper;
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
GRANT ALL ON ALL TABLES IN SCHEMA public TO datascraper;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO datascraper;
