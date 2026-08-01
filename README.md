# Global Business Intelligence Platform

Chrome Extension (Manifest V3 Side Panel) + Spring Boot microservices for worldwide company discovery, multi-source enrichment, normalization, validation, persistence, and enterprise Excel export.

## Architecture

```
Chrome Extension (React + TypeScript + MV3 Side Panel)
        ↓
API Gateway (:8080)
        ↓
Auth (:8081) · Location (:8082) · Company (:8083) · Category (:8084)
Job (:8086) → Discovery (:8087) → Company Queue → Orchestrator (:8085)
        → Data Providers (:8091–8096)
        → Aggregation → Normalization → Validation → Persistence
        → Export (:8088) → Download API → Extension
```

### Plugin architectures

| Layer | Contract | Responsibility |
|-------|----------|----------------|
| Discovery Providers | `DiscoveryProvider` | Discover companies only |
| Data Providers | `CompanyDataProvider` | Enrich discovered companies |

Providers are Strategy + Factory + DI. Enable/disable via configuration. Adding a provider does not modify existing ones.

## Services

| Service | Port | Role |
|---------|------|------|
| gateway-service | 8080 | API entry, CORS, routing |
| auth-service | 8081 | Google/dev OAuth, JWT, refresh |
| location-service | 8082 | ISO-3166 countries, searchable cities |
| company-service | 8083 | Catalog + enriched profile persistence |
| category-service | 8084 | Searchable categories (default: Cleaning) |
| scraper-orchestrator | 8085 | Enrichment pipeline + provider orchestration |
| job-service | 8086 | Async job lifecycle, progress, audit |
| discovery-service | 8087 | Discovery provider plugins |
| export-service | 8088 | SXSSF Excel export (only Excel producer) |
| scraper-website | 8091 | Website signals |
| scraper-tech | 8092 | Technology stack |
| scraper-news | 8093 | News |
| scraper-github | 8094 | GitHub |
| scraper-contact | 8095 | Contacts |
| scraper-social | 8096 | Social profile links |

## Quick start

```powershell
# Infrastructure (Postgres + Redis)
docker compose up -d

# Build + start all services
.\start-platform.ps1

# Extension
cd chrome-extension
npm install
npm run build
# Load unpacked: chrome-extension/dist in chrome://extensions
```

Stop with `.\stop-platform.ps1`.

## User flow

1. Authenticate (Google or dev login)
2. Dashboard — select categories (required; default Cleaning), optional countries/cities
3. **Start Scraping** → job created → immediate redirect to Job Progress
4. Poll progress (Queued → Running → Completed)
5. Download professional Excel workbook from Export / Download Center

## Job lifecycle

`QUEUED` → `RUNNING` → `PAUSED` / `COMPLETED` / `FAILED` / `CANCELLED`

Supports retry, resume, checkpoint recovery, progress %, and ETA.

## Excel export

Apache POI **SXSSF** streaming for large exports. Sheets: Companies, Search Criteria, Export Summary, Job Statistics. Never fabricates data — empty cells when unavailable.

## Configuration

See `.env` for auth/JWT/GitHub. Redis at `localhost:6379` (optional HTTP fallbacks between job → discovery → orchestrator when Redis is down).

## Build

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd test
```
