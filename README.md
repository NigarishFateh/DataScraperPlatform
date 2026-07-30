# Lead Intelligence Platform

Chrome Extension + Spring Boot microservices for European IT company lead intelligence.
Scrapers are organized by data source capability (website, tech, news, GitHub, contact), not by company.

## Documentation

- **[docs/PROJECT_DOCUMENTATION.md](./docs/PROJECT_DOCUMENTATION.md)** — Architecture, APIs, setup
- **[docs/DEPLOYMENT.md](./docs/DEPLOYMENT.md)** — Backend hosting guide

## Quick Start

```powershell
# Start backend services (gateway, auth, scrapers, orchestrator, …)
.\start-platform.ps1

# Chrome extension (separate terminal)
cd chrome-extension
npm install
npm run dev
```

Load the unpacked extension from `chrome-extension/dist` (or follow the extension README).

## Services

| Service | Port |
|---|---|
| Gateway | 8080 |
| Auth | 8081 |
| Location | 8082 |
| Company | 8083 |
| Category | 8084 |
| Orchestrator | 8085 |
| Website scraper | 8091 |
| Tech scraper | 8092 |
| News scraper | 8093 |
| GitHub scraper | 8094 |
| Contact scraper | 8095 |

## Main API

```http
POST http://localhost:8080/api/intelligence/jobs
Content-Type: application/json
X-Correlation-Id: local-1

{
  "companyName": "Example GmbH",
  "websiteUrl": "https://example.com",
  "scraperTypes": ["COMPANY_WEBSITE", "TECHNOLOGY_STACK", "NEWS", "GITHUB", "CONTACT"]
}
```

## Stack

Java 17 · Spring Boot 3 · Maven · Spring Cloud Gateway · WebClient · JSoup · Chrome Extension (React + Vite)
