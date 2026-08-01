-- Create missing BI platform databases (run as postgres superuser)
CREATE DATABASE job_db OWNER datascraper;
CREATE DATABASE discovery_db OWNER datascraper;
CREATE DATABASE export_db OWNER datascraper;

GRANT ALL PRIVILEGES ON DATABASE job_db TO datascraper;
GRANT ALL PRIVILEGES ON DATABASE discovery_db TO datascraper;
GRANT ALL PRIVILEGES ON DATABASE export_db TO datascraper;

\c job_db
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
ALTER SCHEMA public OWNER TO datascraper;

\c discovery_db
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
ALTER SCHEMA public OWNER TO datascraper;

\c export_db
GRANT USAGE, CREATE ON SCHEMA public TO datascraper;
ALTER SCHEMA public OWNER TO datascraper;
