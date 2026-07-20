# Data Scraper Platform — Project Documentation

**Version:** 1.0  
**Stack:** Java 17 · Spring Boot 3.4 · Maven · React · Vite · JSoup  
**Architecture:** Microservices with central Orchestrator  

---

## 1. Executive Summary

The **Data Scraper Platform** is a microservices-based system that scrapes information from IT company websites (Google, Microsoft, IBM).

Users choose:

- which **websites (sources)** to scrape  
- which **categories** of information to collect  

A central **Orchestrator** coordinates specialist scraper services, runs them **in parallel**, and returns a unified JSON response. A **React** frontend provides a simple UI for these operations.

### Goals

| Goal | How it is achieved |
|---|---|
| One website per service | Separate Spring Boot apps for Google, Microsoft, IBM |
| Parallel scraping | `CompletableFuture` + thread pool in Orchestrator |
| Easy extension | New site = new microservice + client registration |
| Flexible data model | Generic `ScrapedData` / `ScrapedItem` for all categories |
| Production readiness | Timeouts, retries, fallbacks, profiles, CORS, Vercel-ready UI |

---

## 2. Problem Statement

Scraping many company websites in one application leads to:

- Tight coupling between unrelated site parsers  
- Hard redeploys when only one site changes  
- Sequential scraping that wastes waiting time  
- Rigid models that break when adding new data types  

This project solves those problems with **microservices**, an **orchestrator pattern**, and a **generic scraped-data model**.

---

## 3. System Architecture

### 3.1 High-level diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend                           │
│              (local :5173 / Vercel production)               │
└───────────────────────────┬─────────────────────────────────┘
                            │ POST /api/scrape
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                 scraper-orchestrator (:8080)                 │
│  Controller → Service → ScraperClientRegistry                │
│  Parallel execution (CompletableFuture)                      │
│  Resilience (timeout / retry / fallback)                     │
└───────────────┬─────────────────┬─────────────────┬─────────┘
                │                 │                 │
                ▼                 ▼                 ▼
     scraper-google         scraper-microsoft    scraper-ibm
         (:8081)                (:8082)            (:8083)
                │                 │                 │
                ▼                 ▼                 ▼
           Google.com        Microsoft.com        IBM.com
```

### 3.2 Why microservices?

| Concern | Monolith | This platform |
|---|---|---|
| Failure isolation | One crash affects all scrapers | One scraper can fail alone |
| Deployment | Redeploy everything | Redeploy one service |
| Scaling | Scale whole app | Scale busy scrapers only |
| Ownership | Shared mixed code | One team / one website |

### 3.3 Service responsibilities

| Service | Responsibility |
|---|---|
| **Orchestrator** | Accept user requests, select scrapers, run in parallel, merge results |
| **Google Scraper** | Download/parse Google pages only |
| **Microsoft Scraper** | Download/parse Microsoft pages only |
| **IBM Scraper** | Download/parse IBM pages only |
| **Frontend** | UI for source/category selection and result display |

---

## 4. Project Structure

```
DataScraperPlatform/
├── pom.xml                          # Parent Maven project
├── start-all-services.ps1           # Launch all backends
├── start-frontend.ps1               # Launch React app
├── README.md / PROJECT_DOCUMENTATION.md
│
├── scraper-orchestrator/            # Coordinator API
│   └── src/main/java/.../orchestrator/
│       ├── controller/
│       ├── service/ + impl/
│       ├── client/                  # WebClient callers + registry
│       ├── config/
│       ├── dto/
│       ├── model/
│       └── exception/
│
├── scraper-google/                  # Google microservice
├── scraper-microsoft/               # Microsoft microservice
├── scraper-ibm/                     # IBM microservice
│   └── each contains:
│       ├── controller/
│       ├── service/ + impl/
│       ├── support/HtmlScrapeParser
│       ├── config/                  # Per-category URLs
│       └── model/
│
└── scraper-frontend/                # React + Vite UI
    ├── src/App.jsx
    ├── src/api/scraperApi.js
    └── vercel.json
```

---

## 5. Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Backend framework | Spring Boot 3.4 | REST APIs, DI, auto-config |
| Language | Java 17 | LTS language features |
| Build | Maven multi-module | Shared versions, modular builds |
| HTML parsing | JSoup | Download + CSS-selector parsing |
| Inter-service HTTP | WebClient | Orchestrator → scrapers |
| Concurrency | CompletableFuture | Parallel scrape tasks |
| Boilerplate reduction | Lombok | Constructors, logging |
| Frontend | React 19 + Vite | Interactive UI |
| Hosting (UI) | Vercel | Static SPA deployment |

---

## 6. Data Model

### 6.1 Categories

```text
JOBS | PRODUCTS | SERVICES | COMPANY_INFO | CONTACTS | NEWS
```

### 6.2 Sources

```text
GOOGLE | MICROSOFT | IBM
```

### 6.3 Core records

**ScrapedItem** — one piece of scraped information:

| Field | Use |
|---|---|
| `title` | Job title, product name, headline, etc. |
| `description` | Longer text when available |
| `url` | Link to source page |
| `location` | Job location / office (when present) |
| `value` | Price, email, phone, date (when present) |
| `metadata` | Extra key/value pairs (type, channel, etc.) |

**ScrapedData** — one scrape result set:

| Field | Use |
|---|---|
| `source` | google / microsoft / ibm |
| `category` | JOBS / PRODUCTS / ... |
| `scrapedAt` | Timestamp |
| `pageTitle` | HTML page title |
| `totalItems` | Count of items |
| `items` | List of ScrapedItem |
| `metadata` | status, targetUrl, error info |

---

## 7. API Specification

### 7.1 Orchestrator

#### Health

```http
GET /api/health
```

Example response:

```json
{
  "service": "scraper-orchestrator",
  "status": "UP",
  "message": "Orchestrator is running and ready to coordinate scraper services."
}
```

#### Scrape

```http
POST /api/scrape
Content-Type: application/json
```

Request:

```json
{
  "sources": ["google", "microsoft"],
  "categories": ["jobs", "news"]
}
```

Notes:

- Body is optional; defaults to all sources + `jobs`.
- Sources and categories must not be empty when provided.
- Response `status` may be `SUCCESS`, `PARTIAL_SUCCESS`, or `FAILED`.

### 7.2 Scraper microservices

```http
GET /api/health
GET /api/scrape/{category}
```

Valid `{category}` values:

```text
jobs | products | services | company_info | contacts | news
```

Ports:

| Service | Base URL (local) |
|---|---|
| Google | `http://localhost:8081` |
| Microsoft | `http://localhost:8082` |
| IBM | `http://localhost:8083` |

---

## 8. Request Lifecycle (End-to-End)

```
1. User clicks "Start Scrape" in React UI
2. Frontend sends POST /api/scrape with sources + categories
3. OrchestratorController validates request (@Valid)
4. OrchestratorServiceImpl expands source × category combinations
5. Each combination runs on scraperExecutor thread pool
6. ScraperClientRegistry selects Google/Microsoft/IBM client
7. AbstractScraperClient calls scraper with timeout + retries
8. Scraper service downloads page (JSoup) and parses by category
9. JSON returns to orchestrator and is aggregated
10. ScrapeResponse returned to frontend and rendered as tables
```

If a scraper fails after retries, a **fallback** `ScrapedData` with `metadata.status = FAILED` is returned so other scrapers can still succeed.

---

## 9. Resilience & Configuration

### Resilience (orchestrator)

| Setting | Default | Meaning |
|---|---|---|
| `scraper.resilience.timeout-ms` | 15000 | Max wait per HTTP call |
| `scraper.resilience.max-retries` | 3 | Attempts before fallback |
| `scraper.resilience.retry-delay-ms` | 1000 | Delay between retries |

### Profiles

| Profile | Purpose |
|---|---|
| `dev` | Local debugging, verbose logs |
| `prod` | Stricter logs, production URLs / CORS |

### CORS

```yaml
app:
  cors:
    allowed-origins: http://localhost:5173,https://your-app.vercel.app
```

---

## 10. Frontend

### Features

- Orchestrator health indicator  
- Multi-select sources  
- Multi-select categories  
- Start scrape action  
- Result summary + per-result tables  

### Local development

```powershell
cd scraper-frontend
npm install
npm run dev
```

Vite proxies `/api` → `http://localhost:8080`.

### Production env

```text
VITE_API_BASE_URL=https://your-orchestrator-public-url
```

---

## 11. How to Run Locally

### Prerequisites

- Java 17+  
- Node.js 18+  
- Windows PowerShell (scripts provided)

### Start backends

```powershell
cd DataScraperPlatform
.\start-all-services.ps1
```

### Start frontend

```powershell
.\start-frontend.ps1
```

### Verify

1. Open `http://localhost:5173`  
2. Confirm **Orchestrator: UP**  
3. Select sources/categories  
4. Click **Start Scrape**

---

## 12. Deployment

### Frontend (Vercel)

1. Import GitHub repo into Vercel.  
2. Set **Root Directory** = `scraper-frontend`.  
3. Set env var `VITE_API_BASE_URL` to public orchestrator URL.  
4. Deploy.

`scraper-frontend/vercel.json` configures Vite build output and SPA routing.

### Backend

Spring Boot services **cannot** run on Vercel. Deploy them to platforms such as:

- Render  
- Railway  
- Fly.io  
- AWS / Azure / GCP  

Then update:

- Orchestrator scraper base URLs  
- CORS allowed origins (include Vercel domain)  
- Frontend `VITE_API_BASE_URL`

---

## 13. Design Principles Applied

| Principle | Application |
|---|---|
| **Single Responsibility** | One scraper service = one website |
| **Open/Closed** | Add scrapers without rewriting existing scrapers |
| **Dependency Inversion** | Controllers depend on interfaces |
| **Separation of Concerns** | Controller / Service / Client / Config |
| **DRY** | Shared client resilience in `AbstractScraperClient` |
| **Fail independently** | Fallbacks + `PARTIAL_SUCCESS` |

---

## 14. Limitations & Future Work

### Current limitations

- JSoup does not execute JavaScript; SPA-heavy pages may return partial data.  
- Parsing heuristics may include navigation noise on some sites.  
- No persistent database / scrape history yet.  
- No authentication / rate limiting yet.

### Suggested next steps

1. Browser automation (Playwright/Selenium) for JS-rendered pages  
2. Persist scrape results (PostgreSQL)  
3. API Gateway + service discovery  
4. Docker Compose for one-command local stack  
5. Auth (JWT) and scrape quotas  
6. Scheduling (cron jobs for periodic scrapes)

---

## 15. Learning Phases Covered

| Phase | Topic |
|---|---|
| 1 | Architecture (monolith vs microservices) |
| 2 | Maven multi-module structure |
| 3 | Orchestrator layered Spring Boot app |
| 4 | First scraper + JSoup |
| 5 | REST + WebClient communication |
| 6 | Adding Microsoft (Open/Closed) |
| 7 | Parallel execution |
| 8 | Generic ScrapedData model |
| 9 | Flexible request DTOs |
| 10 | Resilience patterns |
| 11 | Config, logging, profiles |
| 12 | Architecture review + React UI + deployment readiness |

---

## 16. Quick Reference

| Item | Value |
|---|---|
| Orchestrator | `http://localhost:8080` |
| Google scraper | `http://localhost:8081` |
| Microsoft scraper | `http://localhost:8082` |
| IBM scraper | `http://localhost:8083` |
| Frontend (local) | `http://localhost:5173` |
| Main scrape API | `POST /api/scrape` |
| Health API | `GET /api/health` |

### Example curl

```powershell
curl -Method POST http://localhost:8080/api/scrape `
  -ContentType "application/json" `
  -Body '{"sources":["google","ibm"],"categories":["jobs","news"]}'
```

---

## 17. Conclusion

The Data Scraper Platform demonstrates a clean, extensible microservices design for multi-site scraping:

- clear service boundaries  
- parallel orchestration  
- resilient inter-service communication  
- flexible category-based data model  
- React UI for end users  
- documentation for local and cloud deployment  

It is suitable as a learning project for Spring Boot, microservices architecture, and frontend–backend integration.

---

*Document generated for the DataScraperPlatform project.*
