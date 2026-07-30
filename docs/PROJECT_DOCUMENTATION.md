# Lead Intelligence Platform — Project Documentation

**Stack:** Java 17 · Spring Boot 3.4 · Maven · Spring Cloud Gateway · Chrome Extension (React + Vite) · JSoup  
**Architecture:** Microservices with capability-based scrapers and a central intelligence orchestrator

---

## 1. Executive Summary

The **Lead Intelligence Platform** gathers structured signals about European IT companies for sales/research workflows.

Users (via the Chrome extension) select a company and which **scraper capabilities** to run. The **gateway** authenticates and routes traffic. The **orchestrator** runs selected scrapers in parallel and returns a unified intelligence report.

Scrapers are organized by **data source type**, not by company:

| Scraper | Responsibility |
|---|---|
| `scraper-website` | Company website crawl / page signals |
| `scraper-tech` | Technology stack detection |
| `scraper-news` | News / press mentions |
| `scraper-github` | Public GitHub activity |
| `scraper-contact` | Contact / reachability signals |

---

## 2. System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Chrome Extension (React)                   │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP via Gateway
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    gateway-service (:8080)                   │
│  Routes → auth / location / company / category / orchestrator│
└───────┬──────────┬──────────┬──────────┬────────────────────┘
        │          │          │          │
        ▼          ▼          ▼          ▼
   auth:8081  loc:8082  company:8083  category:8084
                                         │
                                         ▼
                            scraper-orchestrator (:8085)
                                         │
          ┌──────────┬──────────┬────────┼──────────┐
          ▼          ▼          ▼        ▼          ▼
       website    tech       news     github    contact
        :8091     :8092      :8093     :8094     :8095
```

---

## 3. Project Structure

```
DataScraperPlatform/
├── pom.xml
├── start-platform.ps1
├── stop-platform.ps1
├── chrome-extension/           # Lead Intelligence UI
├── platform-common/            # Shared DTOs / enums
├── gateway-service/
├── auth-service/
├── location-service/
├── company-service/
├── category-service/
├── scraper-orchestrator/       # Intelligence job coordinator
├── scraper-website/
├── scraper-tech/
├── scraper-news/
├── scraper-github/
├── scraper-contact/
└── docs/
```

---

## 4. Main API

### Orchestrator health

```http
GET /api/health
```

### Intelligence job

```http
POST /api/intelligence/jobs
Content-Type: application/json
X-Correlation-Id: local-1

{
  "companyName": "Example GmbH",
  "websiteUrl": "https://example.com",
  "scraperTypes": ["COMPANY_WEBSITE", "TECHNOLOGY_STACK", "NEWS", "GITHUB", "CONTACT"]
}
```

`ScraperType` values (from `platform-common`):

```text
COMPANY_WEBSITE | TECHNOLOGY_STACK | NEWS | GITHUB | CONTACT
```

---

## 5. Local Setup

```powershell
# Postgres + Redis as needed for your profile, then:
.\start-platform.ps1

cd chrome-extension
npm install
npm run build
```

Load the unpacked extension from `chrome-extension/dist`.

| Service | Port |
|---|---|
| Gateway | 8080 |
| Auth | 8081 |
| Location | 8082 |
| Company | 8083 |
| Category | 8084 |
| Orchestrator | 8085 |
| Website / Tech / News / GitHub / Contact scrapers | 8091–8095 |

Stop services:

```powershell
.\stop-platform.ps1
```

---

## 6. Design Notes

- **Factory / Strategy:** orchestrator selects scraper adapters by `ScraperType`.
- **Resilience:** timeouts, retries, and fallbacks on remote scraper calls.
- **Cache:** Redis-backed result cache (optional; disabled in default local profile).
- **CORS:** orchestrator allows Chrome extension origins for direct calls when needed.

See also [DEPLOYMENT.md](./DEPLOYMENT.md) for hosting notes.
