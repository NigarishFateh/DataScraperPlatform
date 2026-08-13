c# Implementation Plan & Priorities

Aligned with product requirements in [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md).  
Order is intentional: ship quality Excel for NL brands/branches before deep cleanup polish.

---

## Priority stack (what matters most)

| P | Theme | Why |
|---|--------|-----|
| **P0** | Excel + fields: Address, quality gate, branch rows | Delivers the downloadable outcome users care about |
| **P0** | Apollo + Places pipeline for custom brands → all branches | Core “bakeries in NL” story |
| **P0** | Leadership API (CEO/Founder) maintained & isolated | Explicit product requirement |
| **P1** | NL first in country UX; discovery bias without excluding others | Business focus |
| **P2** | Custom free-text categories | Flexibility beyond seeded list |
| **P2** | Incomplete vs complete Excel sheets / user quality toggle | Honesty when APIs lack data |
| **P3** | Docs/README sync, ignore logs, optional CSV later | Hygiene |

---

## Phase 0 — Hygiene (done / immediate)

- [x] Delete root `*.log` run artifacts  
- [x] Add `*.log` to `.gitignore`  
- [x] Rewrite `docs/PROJECT_DOCUMENTATION.md` for new product scope  
- [x] Refresh root `README.md` to match keep/remove modules  
- [x] **Keep** `docker-compose.yml` (Postgres + Redis). Do **not** add service Dockerfiles until publishing  
- [x] Remove tech / news / github / social scraper modules + orchestrator wiring  

---

## Phase 1 — Export & data contract (P0)

**Goal:** Excel matches the required business columns and branch granularity.

1. [x] Extend `ExcelExportWriter` headers/rows: Address, Branch Name, Branch Manager  
2. [x] Forward Places/Apollo address + phone into export DTO via discovery metadata  
3. [x] Quality sheets: `Branches` (complete) + `Incomplete` (+ `Companies` = all)  
4. [ ] Optional job `options.require*` overrides (defaults applied in export today)

**Done when:** Sample NL bakery job Excel shows address + contact columns; empty critical cells only appear on Incomplete sheet.

---

## Phase 2 — Branch discovery for custom brands (P0)

**Goal:** Named companies in a country → every branch location.

1. [x] Custom discovery: Places branch expansion after Apollo (named mode)  
2. [x] Deduplicate by place_id / address fingerprint  
3. [x] Persist one row per branch location  
4. [x] Custom UI raises `maxCompanies` for branch expansion  

**Done when:** Three NL bakery names → multi-branch rows with addresses/phones where Places returns them.

---

## Phase 3 — Leadership API (P0/P1)

**Goal:** Stable CEO/Founder (and manager) service.

1. Keep `ApolloPeopleLeadershipClient` as primary.  
2. Document credit/plan limits; graceful degrade (FMP → curated → open → Serp) without failing the whole job.  
3. Expose/maintain dedicated endpoint used by Custom + enrichment.  
4. Title ranking: CEO / Founder / Owner → Managing Director → Branch Manager / Supervisor / Bedrijfsleider.  
5. Never write placeholder names (“N/A”, “Unknown”) into CEO column — leave blank and route to Incomplete if required.

**Done when:** Leadership calls are rate-limited, logged, and covered by a short service README in `discovery-service`.

---

## Phase 4 — Netherlands priority (P1)

1. [x] Extension country dropdown: **Netherlands (NL)** pinned at top; rest A–Z.  
2. [x] Custom page defaults country = NL (already).  
3. Discovery must not filter out other countries when selected — unchanged.  
4. Seed/curated leadership lists may stay NL-heavy; other countries use Apollo/Places only.

**Done when:** Selecting DE/US/etc. still returns that country’s data unchanged.

---

## Phase 5 — Architecture cleanup (done)

Removed permanently:

| Removed | Notes |
|---------|--------|
| `scraper-tech` | Module deleted; orchestrator remote client removed |
| `scraper-news` | Module deleted; orchestrator remote client removed |
| `scraper-github` | Module deleted; orchestrator remote client removed |
| `scraper-social` | Module deleted; orchestrator remote client removed |

`ProviderType` / `ScraperType` enums now only `WEBSITE` and `CONTACT`.  
Enrichment providers enabled: website + contact only.

---

## Phase 6 — Categories & limits polish (P2)

1. Allow free-text category keywords on Custom (and optionally Dashboard).  
2. Confirm city remains optional worldwide.  
3. Keep user-controlled `maxCompanies` (document Unlimited = API 100k).  

---

## Phase 7 — Future (backlog)

- CSV export (enum exists, unimplemented)  
- Service Dockerfiles / cloud deploy when a public site exists  
- Cognism or PDL as EU leadership waterfall if Apollo people credits insufficient  
- Rate-limit dashboard + cost telemetry per API key  
- Multi-language category synonyms (NL/EN) for bakeries, cleaning, etc.

---

## Honest constraints (share with stakeholders)

| Claim | Reality |
|-------|---------|
| “No empty fields / no failure” | APIs often lack email or executive name for SMBs. We **maximize fill rate** and **separate Incomplete** — we do not fabricate data. |
| “All branch managers” | Places has address/phone; managers need Apollo People / web — coverage varies. |
| “All world cities” | GeoNames seed is large but not every hamlet; search API covers major populated places. |

---

## Suggested next coding sprint (execute in order)

1. [x] Add Address (+ branch columns) to Excel export.  
2. [x] Wire Places address through to export for every discovered place.  
3. [x] Custom brand → multi-branch Places expansion for selected country.  
4. [x] Quality sheets / filters (`Branches` / `Incomplete`).  
5. [x] NL pin in extension country list.  
6. Leadership API docs + hardening.  
7. Optional: job `options.require*` overrides for export quality.
