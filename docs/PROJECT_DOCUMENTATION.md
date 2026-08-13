# Global Business Scraper — Project Documentation

**Product:** Chrome extension + Spring Boot microservices that discover companies by country/category (or by named brands), enrich contact & leadership data via Apollo + Google Places (+ website contact scraping), and export clean Excel sheets.

**Primary market focus:** Netherlands (NL) first in UX and discovery priority — without blocking any other country.

**Last updated:** 2026-08-10

---

## 1. Product goals (source of truth)

### 1.1 Basic coverage

| # | Requirement | Status today | Target |
|---|-------------|--------------|--------|
| 1 | Access **all countries worldwide** | Done (ISO-3166 seed) | Keep; NL pinned first in UI lists |
| 2 | Cities under each country via API | Done (GeoNames + search API) | Keep; city **optional** |
| 3 | City selection optional | Done | Keep |
| 4 | User-set scrape company limit | Done (UI presets + Unlimited; API cap 100k) | Keep |
| 5 | Categories chosen **before** scrape | Done (dashboard) | Keep; allow custom category text where needed |
| 6 | Fetch: name, city, website, email, phone, CEO/Founder | Partial — fields exist; **Address not in Excel**; empties still possible | **Hard quality gate**: prefer rows with email + phone + address + leadership |
| 7 | Downloadable Excel | Done (`export-service` SXSSF) | Add **Address**, branch, manager columns |

### 1.2 Customized scraping

| # | Requirement | Status today | Target |
|---|-------------|--------------|--------|
| 1 | User writes category + chooses country | Partial — picks seeded categories + free-text **company names** | Support free-text category keywords + country |
| 2 | Scrape all companies given as input | Done (custom scrape / named discovery) | Expand to **every branch** of each named brand |

### 1.3 Unwanted (remove)

| Item | Action |
|------|--------|
| GitHub / News / Tech / Social scrapers | **Removed** from the codebase |
| Root `*.log` run artifacts | Deleted; ignored in git |
| Docker for **publishing apps** | No service Dockerfiles — **keep** `docker-compose.yml` only for local Postgres + Redis |

### 1.4 To implement (core)

1. Apollo + Google Places as primary discovery/enrichment — minimize empty critical fields.
2. Excel **Address** column (data already on `EnrichedCompany.address` in places).
3. NL priority in country list / discovery ordering — other countries unchanged.
4. Quality filter: no empty CEO/email/phone/address **to the best of API capacity** (filter or mark incomplete; do not invent data).
5. **Branches**: one Excel row per branch location; branch contact + branch manager/supervisor when available.
6. Dedicated, maintained **CEO/leadership API** (Apollo People + fallbacks).
7. Custom brand flow example: category *Bakeries*, country *Netherlands*, names *A, B, C* → all branches of A/B/C in Excel.

---

## 2. Current architecture

```
Chrome Extension (MV3 side panel)
  Auth → Dashboard | Custom | Jobs | Exports | Settings
        ↓
API Gateway (:8080)
        ↓
Auth (:8081) · Location (:8082) · Company (:8083) · Category (:8084)
Job (:8086) → Discovery (:8087) → Orchestrator (:8085)
        → Website (:8091) + Contact (:8095) → Persist → Export (:8088)
```

| Service | Port | Keep? | Role |
|---------|------|-------|------|
| gateway-service | 8080 | Yes | Routing, CORS |
| auth-service | 8081 | Yes | Google / dev login, JWT |
| location-service | 8082 | Yes | World countries + cities |
| company-service | 8083 | Yes | Company / location persistence |
| category-service | 8084 | Yes | Category catalog |
| scraper-orchestrator | 8085 | Yes | Enrichment orchestration |
| job-service | 8086 | Yes | Async jobs, progress |
| discovery-service | 8087 | Yes | Apollo, Places, SerpAPI Maps, leadership |
| export-service | 8088 | Yes | Excel only |
| scraper-website | 8091 | Yes | Website identity / contact pages |
| scraper-contact | 8095 | Yes | Emails / phones / address from site |

### Discovery providers (already present)

- **Apollo** — org search + people/leadership client  
- **Google Places** — place search, formatted address, phone, website  
- Fallbacks: SerpAPI Maps, OSM, open leadership, FMP (public tickers)

### Excel columns today

`Company Name | City | Address | Website | Email | Phone Number | Founder / CEO`

Sheets (single category): `Companies` (all) · `With Emails` · `Without Emails`.

---

## 3. Target data model (Excel row)

One row = **one physical branch / location** (not one brand HQ only).

| Column | Source priority |
|--------|-----------------|
| Company / Brand name | Apollo / Places / user input |
| Branch name (if any) | Places display name / Apollo location |
| Country | Job filter |
| City | Places / Apollo / optional filter |
| Address | Places `formattedAddress` → Apollo → website |
| Website | Apollo / Places / site |
| Email | Apollo → website contact scraper |
| Phone | Places / Apollo → website |
| CEO / Founder | Apollo People leadership API |
| Branch manager / Supervisor | Apollo People (title filters) when available |
| Source | apollo / google_places / website |

**Quality rule:** Prefer exporting rows that have the richest contact set. Never fabricate CEO/email/phone/address. Incomplete rows go to a separate sheet (`Incomplete`) or are dropped based on a user toggle (default: require email OR phone + address).

---

## 4. Target user flows

### Dashboard (category scrape)

1. Select country (NL first in list).  
2. Optionally select city.  
3. Select category(ies) **before** start.  
4. Set max companies.  
5. Start job → Discovery (Apollo → Places) → Enrich → Excel.

### Custom scrape

1. Country (+ optional city).  
2. Category keyword(s) (seeded pick and/or free text).  
3. Paste / type brand or company names (e.g. three bakeries).  
4. System resolves **all branches** per brand in that country (Places nearby / brand search + Apollo).  
5. Leadership + contacts per branch where APIs allow.  
6. Excel: clean multi-sheet or single sheet with filters.

---

## 5. Non-goals (explicit)

- Tech stack, news, GitHub, social profile enrichment  
- LinkedIn scraping as a product feature  
- Public website hosting / Kubernetes / service Docker images (until publish)  
- Inventing missing executive names

---

## 6. Local run

```powershell
docker compose up -d          # Postgres + Redis only
.\start-platform.ps1
cd chrome-extension; npm install; npm run build
# Load chrome-extension\dist in chrome://extensions
```

Required secrets in `.env` (examples): `APOLLO_API_KEY`, `GOOGLE_PLACES_API_KEY`, auth/JWT, DB.

**Note:** `docker-compose.yml` is **local infrastructure**, not app publishing. Keep it until a non-Docker Postgres/Redis setup is standard for the team.

---

## 7. Related docs

- [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) — prioritized backlog & architecture cleanup  
- [DEPLOYMENT.md](./DEPLOYMENT.md) — deploy notes (update when publishing)  
- Root [README.md](../README.md) — quick start
