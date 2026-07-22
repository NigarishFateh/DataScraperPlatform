import { useState } from "react";

const SECTIONS = [
  {
    id: "identity",
    title: "Identity",
    summary: "Logo, name, website, headquarters",
    body: "Company identity fields render here after aggregation (Phase 9–13).",
  },
  {
    id: "positioning",
    title: "Positioning",
    summary: "Industry, categories, mission, vision",
    body: "Narrative positioning pulled from public about pages and directories.",
  },
  {
    id: "offerings",
    title: "Offerings",
    summary: "Services and products",
    body: "Structured offerings discovered by the website scraper.",
  },
  {
    id: "technology",
    title: "Technology",
    summary: "Languages, frameworks, cloud, databases",
    body: "Signals from the technology stack scraper and public engineering pages.",
  },
  {
    id: "presence",
    title: "Digital presence",
    summary: "LinkedIn, GitHub, X, careers, news",
    body: "Public profiles and content feeds — LinkedIn only when legally approved.",
  },
  {
    id: "contact",
    title: "Contact",
    summary: "Emails, phones, addresses (public only)",
    body: "Contact scraper results with provenance for each field.",
  },
] as const;

/**
 * Result page shell — expandable intelligence sections.
 * Real report data arrives after scrapers + aggregation.
 */
export function ReportPage() {
  const [openId, setOpenId] = useState<string>("identity");

  return (
    <div className="flex flex-1 flex-col gap-4">
      <section className="li-surface p-4">
        <div className="flex items-start gap-3">
          <div className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-ink-700 font-display text-sm font-semibold text-signal ring-1 ring-white/10">
            LI
          </div>
          <div className="min-w-0 space-y-1">
            <h1 className="font-display text-lg font-semibold text-mist-100">
              Sample Company Report
            </h1>
            <p className="text-xs text-mist-300">example.com · Berlin, Germany · Software</p>
            <p className="text-[11px] text-mist-400">
              Placeholder report — expandable sections preview the final UX.
            </p>
          </div>
        </div>
      </section>

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
                <span
                  className={`text-mist-400 transition ${open ? "rotate-180" : ""}`}
                  aria-hidden
                >
                  ▾
                </span>
              </button>
              {open ? (
                <div className="border-t border-white/10 px-3.5 py-3 text-sm leading-relaxed text-mist-300">
                  {section.body}
                </div>
              ) : null}
            </article>
          );
        })}
      </section>
    </div>
  );
}
