# Global Business Intelligence Platform — Project Documentation

**Stack:** Java 17 · Spring Boot 3.4 · Maven · Spring Cloud Gateway · Redis · PostgreSQL · Chrome Extension (React + Vite) · JSoup · Apache POI SXSSF

---

## 1. Executive Summary

The platform discovers companies worldwide, enriches them from multiple public sources, normalizes and validates records, persists profiles, and exports enterprise Excel workbooks.

Users never pick individual companies. They select categories (required) and optional geography, create an async job, and download results when complete.

---

## 2. System Architecture

```
Chrome Side Panel
      ↓
gateway-service (:8080)
      ├── auth-service (:8081)
      ├── location-service (:8082)   ISO-3166 + cities
      ├── company-service (:8083)   catalog + enriched persistence
      ├── category-service (:8084)  searchable categories
      ├── scraper-orchestrator (:8085)
      ├── job-service (:8086)
      ├── discovery-service (:8087)
      └── export-service (:8088)
                ↑
      Data providers :8091–8096 (website, tech, news, github, contact, social)
```

### Async pipeline

Create Job → Discovery Providers → Company Queue → Parallel Data Providers → Aggregation → Normalization → Validation → Persistence → Export → Download

### Plugin contracts

- `DiscoveryProvider` — discover only
- `CompanyDataProvider` — enrich only

Factory + Strategy + Spring DI. Config toggles enable/disable providers.

---

## 3. Databases

| Database | Owner |
|----------|-------|
| auth_db | auth-service |
| location_db | location-service |
| company_db | company-service |
| category_db | category-service |
| job_db | job-service |
| discovery_db | discovery-service |
| export_db | export-service |

Redis queues: `bi:queue:discovery`, `bi:queue:company`, `bi:queue:export` (HTTP fallbacks when Redis is unavailable).

---

## 4. Chrome Extension

Routes: `/auth`, `/dashboard`, `/jobs`, `/jobs/:id`, `/exports`, `/settings`

Default category: **Cleaning Companies**. Start Scraping creates a job and navigates immediately to progress.

---

## 5. Local operations

```powershell
docker compose up -d
.\start-platform.ps1
cd chrome-extension; npm run build
.\stop-platform.ps1
```

See `docs/DEPLOYMENT.md` for hosted deployment notes.
