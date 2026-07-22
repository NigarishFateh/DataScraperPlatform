import { useMemo, useState } from "react";
import { useLocation } from "react-router-dom";
import { CATEGORIES, COMPANIES, COUNTRIES, CITIES } from "../../data/dummyCatalog";
import { SEARCH_SELECTION_KEY } from "../dashboard/DashboardPage";
import type { DashboardSelection } from "../../types/catalog";

const SECTIONS = [
  {
    id: "identity",
    title: "Identity",
    summary: "Logo, name, website, headquarters",
  },
  {
    id: "positioning",
    title: "Positioning",
    summary: "Industry, categories, mission, vision",
  },
  {
    id: "offerings",
    title: "Offerings",
    summary: "Services and products",
  },
  {
    id: "technology",
    title: "Technology",
    summary: "Languages, frameworks, cloud, databases",
  },
  {
    id: "presence",
    title: "Digital presence",
    summary: "LinkedIn, GitHub, X, careers, news",
  },
  {
    id: "contact",
    title: "Contact",
    summary: "Emails, phones, addresses (public only)",
  },
] as const;

function readSelection(state: unknown): DashboardSelection | null {
  if (state && typeof state === "object" && "companyIds" in state) {
    return state as DashboardSelection;
  }
  try {
    const raw = sessionStorage.getItem(SEARCH_SELECTION_KEY);
    return raw ? (JSON.parse(raw) as DashboardSelection) : null;
  } catch {
    return null;
  }
}

/**
 * Result page — shows Phase 5 selection summary + expandable report shell.
 * Real scraped intelligence arrives in later phases.
 */
export function ReportPage() {
  const location = useLocation();
  const selection = useMemo(() => readSelection(location.state), [location.state]);
  const [openId, setOpenId] = useState<string>("identity");

  const companies = useMemo(() => {
    if (!selection) return [];
    const idSet = new Set(selection.companyIds);
    return COMPANIES.filter((company) => idSet.has(company.id));
  }, [selection]);

  const primary = companies[0];
  const countryName = primary
    ? COUNTRIES.find((country) => country.code === primary.countryCode)?.name
    : null;
  const cityName = primary
    ? CITIES.find((city) => city.id === primary.cityId)?.name
    : null;
  const categoryNames = selection
    ? CATEGORIES.filter((category) => selection.categoryIds.includes(category.id)).map(
        (category) => category.name,
      )
    : [];

  if (!selection || companies.length === 0) {
    return (
      <div className="li-surface space-y-2 p-4 text-sm text-mist-300">
        <p className="font-display font-semibold text-mist-100">No search yet</p>
        <p className="text-[11px] text-mist-400">
          Go to Dashboard, complete the four filters, then click Search.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-1 flex-col gap-4">
      <section className="li-surface p-4">
        <div className="flex items-start gap-3">
          <div className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-ink-700 font-display text-sm font-semibold text-signal ring-1 ring-white/10">
            {primary.name.slice(0, 2).toUpperCase()}
          </div>
          <div className="min-w-0 space-y-1">
            <h1 className="font-display text-lg font-semibold text-mist-100">{primary.name}</h1>
            <p className="text-xs text-mist-300">
              {primary.website.replace(/^https?:\/\//, "")} · {cityName}, {countryName} ·{" "}
              {primary.industry}
            </p>
            <p className="text-[11px] text-mist-400">
              {companies.length} compan{companies.length === 1 ? "y" : "ies"} ·{" "}
              {categoryNames.join(", ") || "No categories"}
            </p>
            <p className="text-[11px] text-signal/90">
              Phase 5 preview — sections below are UX placeholders until scrapers run.
            </p>
          </div>
        </div>
      </section>

      {companies.length > 1 ? (
        <section className="li-surface p-3">
          <p className="mb-2 text-[11px] uppercase tracking-wide text-mist-400">Also selected</p>
          <ul className="space-y-1">
            {companies.slice(1).map((company) => (
              <li key={company.id} className="truncate text-xs text-mist-300">
                {company.name}
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <section className="space-y-2">
        {SECTIONS.map((section, index) => {
          const open = openId === section.id;
          return (
            <article key={section.id} className="li-surface overflow-hidden">
              <button
                type="button"
                className="flex w-full items-center justify-between gap-3 px-3.5 py-3 text-left transition hover:bg-white/[0.03]"
                aria-expanded={open}
                onClick={() => setOpenId(open ? "" : section.id)}
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-display text-[10px] font-semibold text-signal">
                      {String(index + 1).padStart(2, "0")}
                    </span>
                    <h2 className="text-sm font-semibold text-mist-100">{section.title}</h2>
                  </div>
                  <p className="mt-0.5 truncate text-[11px] text-mist-400">{section.summary}</p>
                </div>
                <span className={`text-mist-400 transition ${open ? "rotate-180" : ""}`} aria-hidden>
                  ▾
                </span>
              </button>
              {open ? (
                <div className="border-t border-white/10 px-3.5 py-3 text-sm leading-relaxed text-mist-300">
                  {section.id === "identity" ? (
                    <ul className="space-y-1 text-xs">
                      <li>
                        <span className="text-mist-400">Name:</span> {primary.name}
                      </li>
                      <li>
                        <span className="text-mist-400">Website:</span> {primary.website}
                      </li>
                      <li>
                        <span className="text-mist-400">HQ:</span> {cityName}, {countryName}
                      </li>
                    </ul>
                  ) : section.id === "positioning" ? (
                    <p className="text-xs">
                      Industry <strong className="text-mist-100">{primary.industry}</strong>.
                      Categories: {categoryNames.join(", ")}.
                    </p>
                  ) : (
                    <p className="text-xs text-mist-400">
                      Filled by scrapers + aggregation in later phases.
                    </p>
                  )}
                </div>
              ) : null}
            </article>
          );
        })}
      </section>
    </div>
  );
}
