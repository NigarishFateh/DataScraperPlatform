import { useMutation } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { CategorySelect } from "../../components/filters/CategorySelect";
import { CountryMultiSelect } from "../../components/filters/CountrySelect";
import { FilterSection } from "../../components/filters/FilterSection";
import { Spinner } from "../../components/ui/Spinner";
import { createJob } from "../../services/jobs/jobApi";
import {
  fetchLeadership,
  NL_RESTAURANT_BRANDS,
} from "../../services/leadership/leadershipApi";
import { downloadLeadershipExcel } from "../../services/leadership/leadershipExcel";
import type { LeadershipPerson } from "../../types/leadership";

function parseCompanyNames(raw: string): string[] {
  return raw
    .split(/[\n,;]+/)
    .map((part) => part.trim())
    .filter(Boolean)
    .filter((name, index, all) => all.findIndex((other) => other.toLowerCase() === name.toLowerCase()) === index);
}

export function CustomScrapePage() {
  const navigate = useNavigate();
  const [companyText, setCompanyText] = useState(NL_RESTAURANT_BRANDS.join("\n"));
  const [categoryIds, setCategoryIds] = useState<string[]>([]);
  const [countryCodes, setCountryCodes] = useState<string[]>(["NL"]);
  const [error, setError] = useState<string | null>(null);
  const [leadershipRows, setLeadershipRows] = useState<LeadershipPerson[] | null>(null);
  const [leadershipMeta, setLeadershipMeta] = useState<string | null>(null);

  const companyNames = useMemo(() => parseCompanyNames(companyText), [companyText]);

  const canFetchLeadership = companyNames.length > 0;
  const canScrape = companyNames.length > 0 && categoryIds.length > 0;

  function toggleCategory(categoryId: string) {
    setCategoryIds((prev) =>
      prev.includes(categoryId)
        ? prev.filter((id) => id !== categoryId)
        : [...prev, categoryId],
    );
  }

  function toggleCountry(countryCode: string) {
    setCountryCodes((prev) =>
      prev.includes(countryCode)
        ? prev.filter((code) => code !== countryCode)
        : [...prev, countryCode],
    );
  }

  const leadershipMutation = useMutation({
    mutationFn: () => fetchLeadership(companyNames),
    onSuccess: (data) => {
      setError(null);
      setLeadershipRows(data.results);
      setLeadershipMeta(`${data.found}/${data.requested} leaders found · ${data.notes}`);
    },
    onError: (err) => {
      setError(err instanceof Error ? err.message : "Failed to fetch leadership");
    },
  });

  const scrapeMutation = useMutation({
    mutationFn: () =>
      createJob({
        categoryIds,
        countryCodes,
        cityIds: [],
        maxCompanies: Math.max(companyNames.length, 1),
        companyNames,
        options: { companyNames },
      }),
    onSuccess: (job) => {
      setError(null);
      navigate(`/jobs/${job.id}`, { replace: true });
    },
    onError: (err) => {
      setError(err instanceof Error ? err.message : "Failed to start custom scrape");
    },
  });

  function onDownloadExcel() {
    if (!leadershipRows || leadershipRows.length === 0) {
      return;
    }
    const stamp = new Date().toISOString().slice(0, 10);
    downloadLeadershipExcel(leadershipRows, `leadership-${stamp}.xls`);
  }

  const busy = leadershipMutation.isPending || scrapeMutation.isPending;
  const fetchingLeadership = leadershipMutation.isPending;

  return (
    <div className="flex flex-1 flex-col gap-4">
      <section className="space-y-1">
        <h1 className="font-display text-xl font-semibold tracking-tight text-mist-100">
          Custom scrape
        </h1>
        <p className="text-sm text-mist-300">
          Enter specific companies, pick category and country, then run a full scrape
          (includes CEO / founder) or fetch leadership only.
          Independent from Dashboard filters.
        </p>
      </section>

      <section className="li-surface divide-y divide-white/10">
        <FilterSection
          step="1"
          title="Companies"
          hint="Required · one per line or comma-separated"
        >
          <textarea
            className="min-h-[140px] w-full resize-y rounded-lg border border-white/10 bg-ink-950/60 px-3 py-2 text-sm text-mist-100 placeholder:text-mist-500 focus:border-signal/40 focus:outline-none disabled:opacity-60"
            value={companyText}
            placeholder={"FEBO\nMcDonald's\nKFC"}
            disabled={busy}
            onChange={(event) => setCompanyText(event.target.value)}
          />
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              className="li-btn-ghost !px-2.5 !py-1 text-[11px]"
              disabled={busy}
              onClick={() => setCompanyText(NL_RESTAURANT_BRANDS.join("\n"))}
            >
              Load NL QSR brands
            </button>
            <button
              type="button"
              className="li-btn-ghost !px-2.5 !py-1 text-[11px]"
              disabled={busy}
              onClick={() => {
                setCompanyText("");
                setLeadershipRows(null);
                setLeadershipMeta(null);
              }}
            >
              Clear
            </button>
            <span className="text-[11px] text-mist-400">{companyNames.length} companies</span>
          </div>
        </FilterSection>

        <FilterSection step="2" title="Category" hint="Required for scrape · optional for leadership">
          <CategorySelect selectedIds={categoryIds} onToggle={toggleCategory} />
        </FilterSection>

        <FilterSection step="3" title="Country" hint="Optional · scopes discovery / Apollo location">
          <CountryMultiSelect selectedCodes={countryCodes} onToggle={toggleCountry} />
        </FilterSection>
      </section>

      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        <button
          type="button"
          className="li-btn-ghost disabled:cursor-not-allowed disabled:opacity-50"
          disabled={!canFetchLeadership || busy}
          onClick={() => leadershipMutation.mutate()}
        >
          {fetchingLeadership ? (
            <span className="inline-flex items-center gap-2">
              <Spinner size="sm" />
              Fetching leaders…
            </span>
          ) : (
            "Fetch leadership"
          )}
        </button>
        <button
          type="button"
          className="li-btn-primary disabled:cursor-not-allowed disabled:opacity-50"
          disabled={!canScrape || busy}
          onClick={() => scrapeMutation.mutate()}
        >
          {scrapeMutation.isPending ? "Starting…" : "Start custom scrape"}
        </button>
      </div>

      {error ? <p className="text-[11px] text-red-300">{error}</p> : null}

      {fetchingLeadership ? (
        <section
          className="li-surface flex flex-col items-center justify-center gap-3 px-4 py-10"
          aria-busy="true"
          aria-live="polite"
        >
          <Spinner size="lg" />
          <div className="space-y-1 text-center">
            <p className="font-display text-sm font-semibold text-mist-100">
              Fetching leadership…
            </p>
            <p className="text-[11px] text-mist-400">
              Looking up CEOs / founders for {companyNames.length}{" "}
              {companyNames.length === 1 ? "company" : "companies"}. This can take a minute.
            </p>
          </div>
          <div className="mt-1 h-1 w-40 overflow-hidden rounded-full bg-white/10">
            <div className="h-full w-1/2 animate-[progress-indeterminate_1.4s_ease-in-out_infinite] rounded-full bg-signal" />
          </div>
        </section>
      ) : null}

      {leadershipMeta && !fetchingLeadership ? (
        <div className="flex items-center justify-between gap-2">
          <p className="text-[11px] leading-relaxed text-mist-400">{leadershipMeta}</p>
          <button
            type="button"
            className="li-btn-ghost !px-2.5 !py-1 text-[11px] disabled:opacity-50"
            disabled={!leadershipRows?.length}
            onClick={onDownloadExcel}
          >
            Download Excel
          </button>
        </div>
      ) : null}

      {leadershipRows && !fetchingLeadership ? (
        <section className="li-surface overflow-hidden">
          <div className="border-b border-white/10 px-3.5 py-2.5">
            <h2 className="text-sm font-semibold text-mist-100">Leadership results</h2>
          </div>
          <div className="max-h-80 overflow-auto">
            <table className="w-full text-left text-[11px]">
              <thead className="sticky top-0 bg-ink-900/95 text-mist-400">
                <tr>
                  <th className="px-3 py-2 font-medium">Brand</th>
                  <th className="px-3 py-2 font-medium">Leader</th>
                  <th className="px-3 py-2 font-medium">Title</th>
                  <th className="px-3 py-2 font-medium">Source</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {leadershipRows.map((row) => (
                  <tr key={row.companyName} className="text-mist-100">
                    <td className="px-3 py-2 align-top">{row.companyName}</td>
                    <td className="px-3 py-2 align-top">
                      {row.found ? row.leaderName : "—"}
                    </td>
                    <td className="px-3 py-2 align-top text-mist-300">
                      {row.found ? row.leadershipTitle : "—"}
                    </td>
                    <td className="px-3 py-2 align-top text-mist-400">
                      {row.source}
                      {row.ticker ? ` · ${row.ticker}` : ""}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}

      {!fetchingLeadership && !leadershipRows ? (
        <p className="text-[11px] leading-relaxed text-mist-400">
          Fetch leadership for CEO / founder signals (FMP, Wikipedia, curated). Start custom scrape
          to discover + enrich the same company list through the normal job pipeline.
        </p>
      ) : null}
    </div>
  );
}
