-- Run once as a PostgreSQL superuser (e.g. postgres):
--   psql -U postgres -f infra/postgres/setup-native-postgres.sql

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'datascraper') THEN
        CREATE ROLE datascraper LOGIN PASSWORD 'datascraper';
    END IF;
END
$$;

ALTER ROLE datascraper WITH PASSWORD 'datascraper';

SELECT 'CREATE DATABASE location_db OWNER datascraper'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'location_db')\gexec
SELECT 'CREATE DATABASE company_db OWNER datascraper'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'company_db')\gexec
SELECT 'CREATE DATABASE auth_db OWNER datascraper'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db')\gexec
SELECT 'CREATE DATABASE category_db OWNER datascraper'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'category_db')\gexec
SELECT 'CREATE DATABASE job_db OWNER datascraper'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'job_db')\gexec
SELECT 'CREATE DATABASE discovery_db OWNER datascraper'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'discovery_db')\gexec
SELECT 'CREATE DATABASE export_db OWNER datascraper'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'export_db')\gexec

GRANT ALL PRIVILEGES ON DATABASE location_db TO datascraper;
GRANT ALL PRIVILEGES ON DATABASE company_db TO datascraper;
GRANT ALL PRIVILEGES ON DATABASE auth_db TO datascraper;
GRANT ALL PRIVILEGES ON DATABASE category_db TO datascraper;
GRANT ALL PRIVILEGES ON DATABASE job_db TO datascraper;
GRANT ALL PRIVILEGES ON DATABASE discovery_db TO datascraper;
GRANT ALL PRIVILEGES ON DATABASE export_db TO datascraper;

-- PostgreSQL 15+ : non-owners need explicit CREATE on schema public
\c location_db
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
ALTER SCHEMA public OWNER TO datascraper;
GRANT ALL ON ALL TABLES IN SCHEMA public TO datascraper;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO datascraper;

\c company_db
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
ALTER SCHEMA public OWNER TO datascraper;
GRANT ALL ON ALL TABLES IN SCHEMA public TO datascraper;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO datascraper;

\c auth_db
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
ALTER SCHEMA public OWNER TO datascraper;
GRANT ALL ON ALL TABLES IN SCHEMA public TO datascraper;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO datascraper;

\c category_db
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
ALTER SCHEMA public OWNER TO datascraper;
GRANT ALL ON ALL TABLES IN SCHEMA public TO datascraper;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO datascraper;

\c job_db
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
ALTER SCHEMA public OWNER TO datascraper;
GRANT ALL ON ALL TABLES IN SCHEMA public TO datascraper;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO datascraper;

\c discovery_db
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
ALTER SCHEMA public OWNER TO datascraper;
GRANT ALL ON ALL TABLES IN SCHEMA public TO datascraper;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO datascraper;

\c export_db
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
ALTER SCHEMA public OWNER TO datascraper;
GRANT ALL ON ALL TABLES IN SCHEMA public TO datascraper;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO datascraper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO datascraper;
