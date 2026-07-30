import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useLocation } from "react-router-dom";
import { SEARCH_SELECTION_KEY, type SearchPayload } from "../dashboard/DashboardPage";
import { createIntelligenceJob } from "../../services/intelligence/intelligenceApi";
import {
  buildSectionView,
  type ReportSectionId,
} from "../../services/intelligence/reportMapper";
import type { Category, City, Company, DashboardSelection } from "../../types/catalog";

const SECTIONS: {
  id: ReportSectionId;
  title: string;
  summary: string;
}[] = [
  {
    id: "identity",
    title: "Identity",
    summary: "Name, website signals, and public branding",
  },
  {
    id: "positioning",
    title: "Positioning",
    summary: "Headings and about copy from the company site",
  },
  {
    id: "offerings",
    title: "Offerings",
    summary: "Services and product links",
  },
  {
    id: "technology",
    title: "Technology",
    summary: "Detected languages, frameworks, and cloud signals",
  },
  {
    id: "presence",
    title: "Digital presence",
    summary: "Social profiles, GitHub, careers, and news",
  },
  {
    id: "contact",
    title: "Contact",
    summary: "Public emails, phones, and addresses",
  },
];

function readSearchContext(state: unknown): {
  selection: DashboardSelection;
  companies: Company[];
  categories: Category[];
  countryName: string | null;
  cities: City[];
} | null {
  const fromState = state as SearchPayload | null;
  if (fromState?.companyIds?.length) {
    return {
      selection: fromState,
      companies: fromState.companies ?? [],
      categories: fromState.categories ?? [],
      countryName: fromState.countryName ?? null,
      cities: fromState.cities ?? [],
    };
  }
  try {
    const raw = sessionStorage.getItem(SEARCH_SELECTION_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as SearchPayload;
    return {
      selection: parsed,
      companies: parsed.companies ?? [],
      categories: parsed.categories ?? [],
      countryName: parsed.countryName ?? null,
      cities: parsed.cities ?? [],
    };
  } catch {
    return null;
  }
}

export function ReportPage() {
  const location = useLocation();
  const context = useMemo(() => readSearchContext(location.state), [location.state]);
  const selection = context?.selection ?? null;
  const companies = context?.companies ?? [];
  const categories = context?.categories ?? [];
  const countryName = context?.countryName ?? null;
  const cities = context?.cities ?? [];
  const [openId, setOpenId] = useState<string>("identity");

  const primary = companies[0];

  const intelligenceQuery = useQuery({
    queryKey: ["intelligence", primary?.id, selection?.categoryIds],
    queryFn: () =>
      createIntelligenceJob({
        companyId: primary!.id,
        companyName: primary!.name,
        websiteUrl: primary!.website,
        categoryIds: selection!.categoryIds,
      }),
    enabled: Boolean(primary && selection),
    staleTime: 60_000,
  });

  const results = intelligenceQuery.data?.results ?? [];

  const cityName =
    cities.find((city) => city.id === primary?.cityId)?.name ??
    primary?.cityId.replace(/^[^-]+-/, "").replace(/-/g, " ") ??
    null;

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
              <a
                href={primary.website}
                target="_blank"
                rel="noreferrer"
                className="text-signal underline-offset-2 hover:underline"
              >
                {primary.website.replace(/^https?:\/\//, "")}
              </a>
              {" · "}
              {cityName}
              {countryName ? `, ${countryName}` : primary?.countryCode ? `, ${primary.countryCode}` : ""} ·{" "}
              {primary.industry}
            </p>
            {categories.length > 0 ? (
              <div className="flex flex-wrap gap-1.5 pt-1">
                {categories.map((category) => (
                  <span
                    key={category.id}
                    className="rounded-md border border-white/10 bg-white/5 px-2 py-0.5 text-[10px] text-mist-300"
                  >
                    {category.name}
                  </span>
                ))}
              </div>
            ) : null}
            <p className="text-[11px] text-signal/90">
              {intelligenceQuery.isLoading
                ? "Running intelligence job…"
                : intelligenceQuery.isError
                  ? "Intelligence job failed — showing catalog preview."
                  : intelligenceQuery.data
                    ? `Job ${intelligenceQuery.data.status} · ${intelligenceQuery.data.elapsedMs}ms`
                    : "Intelligence job pending"}
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
          const view = buildSectionView(section.id, results);
          const catalogFallback =
            section.id === "identity" && view.items.length === 0
              ? [
                  { label: "Name", value: primary.name },
                  { label: "Website", value: primary.website, href: primary.website },
                  {
                    label: "Headquarters",
                    value: `${cityName ?? ""}${
                      countryName
                        ? `, ${countryName}`
                        : primary.countryCode
                          ? `, ${primary.countryCode}`
                          : ""
                    }`,
                  },
                  { label: "Industry", value: primary.industry },
                ]
              : null;

          const groups =
            view.groups.length > 0
              ? view.groups
              : catalogFallback
                ? catalogFallback.map((row, i) => ({
                    id: `fallback-${i}`,
                    label: row.label,
                    values: [
                      {
                        id: `fallback-${i}-value`,
                        value: row.value,
                        href: "href" in row ? row.href : undefined,
                        note: undefined as string | undefined,
                      },
                    ],
                  }))
                : [];

          const valueCount = groups.reduce((sum, g) => sum + g.values.length, 0);

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
                    {valueCount > 0 ? (
                      <span className="rounded-full bg-white/5 px-2 py-0.5 text-[10px] text-mist-400">
                        {valueCount}
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-0.5 truncate text-[11px] text-mist-400">{section.summary}</p>
                </div>
                <span className={`text-mist-400 transition ${open ? "rotate-180" : ""}`} aria-hidden>
                  ▾
                </span>
              </button>
              {open ? (
                <div className="border-t border-white/10 px-3.5 py-3">
                  {view.statusLabel ? (
                    <p className="mb-3 text-[10px] text-mist-500">{view.statusLabel}</p>
                  ) : null}

                  {groups.length > 0 ? (
                    <ul className="space-y-4">
                      {groups.map((group) => (
                        <li key={group.id} className="border-b border-white/5 pb-4 last:border-0 last:pb-0">
                          <p className="mb-1.5 text-[10px] font-medium uppercase tracking-wide text-mist-500">
                            {group.label}
                          </p>
                          <ul className="space-y-1.5">
                            {group.values.map((row) => (
                              <li key={row.id}>
                                {row.href ? (
                                  <a
                                    href={row.href}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="block break-words text-xs leading-relaxed text-signal underline-offset-2 hover:underline"
                                  >
                                    {row.value}
                                  </a>
                                ) : (
                                  <p className="break-words text-xs leading-relaxed text-mist-100">
                                    {row.value}
                                  </p>
                                )}
                                {row.note ? (
                                  <p className="mt-0.5 text-[10px] text-mist-500">{row.note}</p>
                                ) : null}
                              </li>
                            ))}
                          </ul>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-xs text-mist-400">
                      {intelligenceQuery.isLoading ? "Waiting for scraper results…" : view.emptyMessage}
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
