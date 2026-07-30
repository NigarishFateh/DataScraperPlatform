# Deployment Guide

Lead Intelligence Platform: Spring Boot microservices + Chrome extension client.
The extension talks to the **gateway**; the gateway routes to auth, company, category, location, and the intelligence orchestrator.

## Architecture

```
Chrome Extension → Gateway (:8080)
                     ├── Auth / Location / Company / Category
                     └── Orchestrator (:8085)
                           ├── scraper-website (:8091)
                           ├── scraper-tech    (:8092)
                           ├── scraper-news    (:8093)
                           ├── scraper-github  (:8094)
                           └── scraper-contact (:8095)
```

| Component | Where to deploy | Notes |
|---|---|---|
| `chrome-extension` | Chrome Web Store / unpacked load | Client UI |
| `gateway-service` | Render, Railway, Fly.io, AWS, etc. | Public entrypoint |
| `auth-service` | Same | OAuth / JWT |
| `location-service` | Same | |
| `company-service` | Same | |
| `category-service` | Same | |
| `scraper-orchestrator` | Same | Coordinates capability scrapers |
| `scraper-website` / `tech` / `news` / `github` / `contact` | Same | Called via orchestrator `base-url` |

---

## Step 1 — Build backends

```powershell
mvn clean package -DskipTests
```

JARs are under each module's `target/` folder, for example:

- `gateway-service/target/gateway-service-*.jar`
- `scraper-orchestrator/target/scraper-orchestrator-*.jar`
- `scraper-website/target/scraper-website-*.jar`
- …and the other services listed above

Run services with the **prod** profile. Point the orchestrator at scraper URLs in env or `application-prod.yml`:

```yaml
scraper:
  services:
    website:
      base-url: https://your-website-scraper.example.com
    tech:
      base-url: https://your-tech-scraper.example.com
    news:
      base-url: https://your-news-scraper.example.com
    github:
      base-url: https://your-github-scraper.example.com
    contact:
      base-url: https://your-contact-scraper.example.com

app:
  cors:
    allowed-origins: chrome-extension://YOUR_EXTENSION_ID
```

Verify health:

```bash
curl https://your-orchestrator.example.com/api/health
```

---

## Step 2 — Local development

```powershell
.\start-platform.ps1

cd chrome-extension
npm install
npm run build
```

Load the unpacked extension from `chrome-extension/dist`. The extension should target the gateway at `http://localhost:8080`.

---

## Step 3 — Verify

1. Sign in via the extension (auth-service through gateway).
2. Run an intelligence job (`POST /api/intelligence/jobs`).
3. Confirm scraper results appear in the report UI.

---

## Troubleshooting

### Orchestrator health DOWN

- Confirm the orchestrator process is listening (default local port **8085**).
- Confirm gateway `ORCHESTRATOR_URI` points at that host.
- Check `/api/health` returns JSON with `"status":"UP"`.

### CORS errors from the extension

- Add your extension origin (`chrome-extension://…`) to `app.cors.allowed-origins` on the orchestrator (and gateway if applicable).

### Scraper timeouts

- Increase `scraper.resilience.timeout-ms` / `scraper.execution.job-timeout-ms` on the orchestrator.
- Confirm each capability scraper JAR is running and reachable from the orchestrator.
