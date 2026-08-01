import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { CategorySelect } from "../../components/filters/CategorySelect";
import { CityMultiSelect } from "../../components/filters/CityMultiSelect";
import { CountryMultiSelect } from "../../components/filters/CountrySelect";
import { FilterSection } from "../../components/filters/FilterSection";
import { useDashboardFilters } from "../../hooks/useDashboardFilters";
import { createJob } from "../../services/jobs/jobApi";
import {
  appendSearchHistory,
  createSavedSearch,
  saveSavedSearch,
} from "../../services/storage/settingsStorage";

export function DashboardPage() {
  const navigate = useNavigate();
  const filters = useDashboardFilters();
  const [error, setError] = useState<string | null>(null);
  const [saveName, setSaveName] = useState("");
  const [showSaveForm, setShowSaveForm] = useState(false);

  const startMutation = useMutation({
    mutationFn: () =>
      createJob({
        categoryIds: filters.filters.categoryIds,
        countryCodes: filters.filters.countryCodes,
        cityIds: filters.filters.cityIds,
        maxCompanies: filters.filters.maxCompanies,
      }),
    onSuccess: async (job) => {
      setError(null);
      await appendSearchHistory(filters.filters, job.id);
      navigate(`/jobs/${job.id}`, { replace: true });
    },
    onError: (err) => {
      setError(err instanceof Error ? err.message : "Failed to start scraping job");
    },
  });

  async function onSaveSearch() {
    const name = saveName.trim();
    if (!name) return;
    await saveSavedSearch(createSavedSearch(name, filters.filters));
    setSaveName("");
    setShowSaveForm(false);
  }

  return (
    <div className="flex flex-1 flex-col gap-4">
      <section className="space-y-1">
        <h1 className="font-display text-xl font-semibold tracking-tight text-mist-100">
          New scrape
        </h1>
        <p className="text-sm text-mist-300">
          Configure filters and start an asynchronous business intelligence job.
        </p>
      </section>

      <section className="li-surface divide-y divide-white/10">
        <FilterSection
          step="1"
          title="Categories"
          hint="Required · searchable multi-select"
          status={!filters.defaultLoaded ? "Loading" : undefined}
        >
          <CategorySelect
            selectedIds={filters.categoryIds}
            onToggle={filters.toggleCategory}
          />
        </FilterSection>

        <FilterSection
          step="2"
          title="Countries"
          hint="Optional · leave empty for global catalog (seed coverage: US, GB, IN, DE)"
        >
          <CountryMultiSelect
            selectedCodes={filters.countryCodes}
            onToggle={filters.toggleCountry}
          />
        </FilterSection>

        <FilterSection step="3" title="Cities" hint="Optional · searchable · no country required">
          <CityMultiSelect selectedIds={filters.cityIds} onToggle={filters.toggleCity} />
        </FilterSection>

        <FilterSection step="4" title="Volume" hint="Maximum companies to process">
          <label className="block">
            <span className="sr-only">Max companies</span>
            <input
              type="number"
              min={1}
              max={5000}
              value={filters.maxCompanies}
              onChange={(event) => filters.setMaxCompanies(Number(event.target.value) || 200)}
              className="w-full rounded-lg border border-white/10 bg-ink-900/80 px-3 py-2.5 text-sm text-mist-100 outline-none focus:border-signal/50"
            />
          </label>
        </FilterSection>
      </section>

      <button
        type="button"
        className="li-btn-primary disabled:cursor-not-allowed disabled:opacity-50"
        disabled={!filters.canStart || startMutation.isPending}
        onClick={() => startMutation.mutate()}
      >
        {startMutation.isPending ? "Starting…" : "Start Scraping"}
      </button>

      {error ? <p className="text-[11px] text-red-300">{error}</p> : null}

      <div className="space-y-2">
        {showSaveForm ? (
          <div className="li-surface flex gap-2 p-3">
            <input
              type="text"
              value={saveName}
              onChange={(event) => setSaveName(event.target.value)}
              placeholder="Saved search name"
              className="min-w-0 flex-1 rounded-lg border border-white/10 bg-ink-900/80 px-3 py-2 text-sm text-mist-100 outline-none focus:border-signal/50"
            />
            <button type="button" className="li-btn-ghost !px-3" onClick={() => void onSaveSearch()}>
              Save
            </button>
            <button
              type="button"
              className="li-btn-ghost !px-3"
              onClick={() => setShowSaveForm(false)}
            >
              Cancel
            </button>
          </div>
        ) : (
          <button
            type="button"
            className="li-btn-ghost w-full text-xs"
            onClick={() => setShowSaveForm(true)}
          >
            Save current filters
          </button>
        )}
      </div>

      <p className="text-[11px] leading-relaxed text-mist-400">
        Jobs run asynchronously — you&apos;ll land on progress immediately after creation.
      </p>
    </div>
  );
}
