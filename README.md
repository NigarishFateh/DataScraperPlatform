# Global Business Scraper

Chrome Extension (Manifest V3 side panel) + Spring Boot microservices for **worldwide** company discovery (Netherlands-first UX), Apollo + Google Places enrichment, and **Excel export** of company / branch contacts.

## What it does

1. Pick country (optional city) → category → company limit → scrape  
2. Or **Custom**: country + category + named brands → all matching **branches**  
3. Download Excel: name, city, address, website, email, phone, CEO/founder (+ manager when available)

## Architecture

```
Chrome Extension
      ↓
Gateway (:8080)
      ↓
Auth · Location · Company · Category · Job · Discovery · Orchestrator · Export
      ↓
Website + Contact providers only (tech/news/github/social removed)
```

Primary discovery/enrichment: **Apollo API** + **Google Places API**.

## Docs

- [Product requirements & current vs target](docs/PROJECT_DOCUMENTATION.md)  
- [Priorities & implementation phases](docs/IMPLEMENTATION_PLAN.md)  
- [Deployment notes](docs/DEPLOYMENT.md)

## Quick start

```powershell
docker compose up -d          # local Postgres + Redis only
.\start-platform.ps1

cd chrome-extension
npm install
npm run build
# Load chrome-extension\dist in chrome://extensions
```

Configure `.env` with `APOLLO_API_KEY`, `GOOGLE_PLACES_API_KEY`, DB, and auth secrets.

## Module status

| Keep |
|------|
| gateway, auth, location, company, category, job, discovery, orchestrator, export |
| scraper-website, scraper-contact |

Removed: scraper-tech, scraper-news, scraper-github, scraper-social.

`docker-compose.yml` stays for local DB/Redis — not used for publishing a website yet.
