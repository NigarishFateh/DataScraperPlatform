# Deployment Guide

This platform has **4 Spring Boot backends** and **1 React frontend**. Vercel hosts **only the React frontend**.

## Architecture on Vercel

```
Browser → Vercel (React static + /api proxy) → Orchestrator (public URL)
                                              → Google / Microsoft / IBM scrapers
```

| Component | Where to deploy | Notes |
|---|---|---|
| `scraper-frontend` | **Vercel** | Static Vite build + serverless `/api` proxy |
| `scraper-orchestrator` | Render, Railway, Fly.io, AWS, etc. | Must be publicly reachable |
| `scraper-google` | Same as above | Orchestrator calls via `base-url` |
| `scraper-microsoft` | Same as above | |
| `scraper-ibm` | Same as above | |

---

## Step 1 — Deploy backends

Build each service as a JAR and deploy to your cloud provider:

```powershell
mvn clean package -DskipTests
```

JARs are produced under each module's `target/` folder:

- `scraper-orchestrator/target/scraper-orchestrator-*.jar`
- `scraper-google/target/scraper-google-*.jar`
- `scraper-microsoft/target/scraper-microsoft-*.jar`
- `scraper-ibm/target/scraper-ibm-*.jar`

Run each with the **prod** profile and set scraper URLs in environment variables or `application-prod.yml`:

```yaml
scraper:
  services:
    google:
      base-url: https://your-google-scraper.example.com
    microsoft:
      base-url: https://your-microsoft-scraper.example.com
    ibm:
      base-url: https://your-ibm-scraper.example.com

app:
  cors:
    allowed-origins: https://your-app.vercel.app
```

Or override via env (Spring relaxed binding):

```bash
APP_CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
SCRAPER_SERVICES_GOOGLE_BASE_URL=https://...
```

Verify the orchestrator is live:

```bash
curl https://your-orchestrator.example.com/api/health
```

---

## Step 2 — Deploy frontend to Vercel

### Option A — Vercel CLI

```powershell
cd scraper-frontend
npm install -g vercel
vercel
```

Follow prompts. Set **Root Directory** to `scraper-frontend` if deploying from the monorepo root.

### Option B — GitHub import

1. Push this repo to GitHub.
2. Go to [vercel.com/new](https://vercel.com/new).
3. Import the repository.
4. Set **Root Directory** → `scraper-frontend`.
5. Framework Preset: **Vite** (auto-detected from `vercel.json`).
6. Deploy.

---

## Step 3 — Configure Vercel environment variables

In **Vercel → Project → Settings → Environment Variables**:

| Variable | Required | Description |
|---|---|---|
| `ORCHESTRATOR_URL` | **Yes** (recommended) | Public orchestrator URL, no trailing slash. Used by serverless proxy at `/api/*`. |
| `VITE_API_BASE_URL` | No | If set, browser calls orchestrator directly (skip proxy). Requires CORS on orchestrator. |

**Recommended (proxy mode):**

```
ORCHESTRATOR_URL=https://your-orchestrator.onrender.com
```

Leave `VITE_API_BASE_URL` unset. The frontend calls same-origin `/api/health` and `/api/scrape`, which Vercel proxies to your orchestrator.

**Alternative (direct mode):**

```
VITE_API_BASE_URL=https://your-orchestrator.onrender.com
```

Add your Vercel domain to orchestrator CORS (`app.cors.allowed-origins`).

Redeploy after changing env vars.

---

## Step 4 — Verify production

1. Open your Vercel URL (e.g. `https://your-app.vercel.app`).
2. Header should show **Orchestrator: UP** and **API: Vercel proxy → ORCHESTRATOR_URL**.
3. Select sources/categories and click **Start Scrape**.
4. Results should appear with `status: SUCCESS`.

---

## Local development (unchanged)

```powershell
.\start-all-services.ps1
.\start-frontend.ps1
```

Open **http://localhost:5173**. Vite proxies `/api` to `localhost:8080`.

Copy `scraper-frontend/.env.example` to `.env.local` if you need custom local settings.

---

## Files added for Vercel

| File | Purpose |
|---|---|
| `scraper-frontend/vercel.json` | Build config, SPA rewrites, scrape timeout |
| `scraper-frontend/api/health.js` | Serverless proxy → orchestrator health |
| `scraper-frontend/api/scrape.js` | Serverless proxy → orchestrator scrape |
| `scraper-frontend/.env.example` | Env var documentation |

---

## Troubleshooting

### Orchestrator shows DOWN on Vercel

- Confirm `ORCHESTRATOR_URL` is set and has **no trailing slash**.
- Confirm orchestrator is running and `/api/health` returns JSON.
- Check Vercel **Functions** logs for proxy errors.

### CORS errors in browser console

- If using `VITE_API_BASE_URL`, add your Vercel URL to `app.cors.allowed-origins` on the orchestrator.
- Prefer proxy mode (leave `VITE_API_BASE_URL` empty) to avoid CORS.

### Scrape times out on Vercel

- Full multi-source scrapes can exceed **10 seconds** (Vercel Hobby limit).
- Upgrade to Vercel Pro for 60s function timeout (configured in `vercel.json`).
- Or reduce selected sources/categories for faster runs.

### 404 on page refresh

- `vercel.json` includes SPA rewrites; ensure Root Directory is `scraper-frontend`.

---

## Summary

| Environment | API path | Backend |
|---|---|---|
| Local dev | `/api/*` via Vite proxy | `localhost:8080` |
| Vercel (proxy) | `/api/*` via serverless | `ORCHESTRATOR_URL` |
| Vercel (direct) | `VITE_API_BASE_URL/api/*` | Public orchestrator + CORS |
