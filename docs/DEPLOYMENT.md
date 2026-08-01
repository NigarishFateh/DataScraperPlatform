# Deployment

## Local

```powershell
docker compose up -d
.\setup-postgres.ps1   # native Postgres only
.\start-platform.ps1
cd chrome-extension; npm install; npm run build
```

Load `chrome-extension/dist` as an unpacked extension.

## Hosted services

Deploy each Spring Boot jar independently (Render, Railway, ECS, etc.).

| Service | Env highlights |
|---------|----------------|
| auth-service | `AUTH_DB_*`, `AUTH_JWT_SECRET`, `GOOGLE_CLIENT_IDS` |
| job-service | `JOB_DB_*`, `REDIS_*`, `DISCOVERY_SERVICE_URI` |
| discovery-service | `COMPANY_SERVICE_URI`, `ORCHESTRATOR_URI`, Redis |
| scraper-orchestrator | scraper base URLs, `COMPANY_SERVICE_URI`, `EXPORT_SERVICE_URI` |
| export-service | `EXPORT_STORAGE_PATH`, `EXPORT_DB_*` |
| gateway-service | upstream `*_SERVICE_URI` |

Point the extension `VITE_API_BASE_URL` at the gateway. Update Chrome OAuth client origins/redirects for production.

## Redis

Required for horizontal scale. Without Redis, job → discovery → orchestrator use HTTP fallbacks (single-node friendly).

## Observability

Each service exposes `/actuator/health` (or `/api/health`). Gateway forwards correlation IDs via `X-Correlation-Id`.
