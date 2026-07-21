# Data Scraper Platform

Microservices platform that scrapes IT company websites (Google, Microsoft, IBM) using Spring Boot, with a React UI.

## Documentation

- **[PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md)** — Full project document (architecture, APIs, setup, deployment)
- **[DEPLOYMENT.md](./DEPLOYMENT.md)** — Vercel frontend + backend hosting guide

## Quick Start

```powershell
# Start all backend services
.\start-all-services.ps1

# Start React frontend
.\start-frontend.ps1
```

Open **http://localhost:5173**

## Services

| Service | Port |
|---|---|
| Orchestrator | 8080 |
| Google scraper | 8081 |
| Microsoft scraper | 8082 |
| IBM scraper | 8083 |
| React frontend | 5173 |

## Main API

```http
POST http://localhost:8080/api/scrape
Content-Type: application/json

{
  "sources": ["google", "microsoft", "ibm"],
  "categories": ["jobs", "products", "news"]
}
```

## Stack

Java 17 · Spring Boot 3 · Maven · WebClient · JSoup · React · Vite

## Deploy to Vercel

Frontend only — see **[DEPLOYMENT.md](./DEPLOYMENT.md)** for full steps.

1. Deploy orchestrator + scrapers to Render/Railway/etc.
2. Import repo on Vercel with **Root Directory** = `scraper-frontend`
3. Set `ORCHESTRATOR_URL` to your public orchestrator URL
4. Deploy
