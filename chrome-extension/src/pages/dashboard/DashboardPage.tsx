import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { CategorySelect } from "../../components/filters/CategorySelect";
import { CityMultiSelect } from "../../components/filters/CityMultiSelect";
import { CountryMultiSelect } from "../../components/filters/CountrySelect";
import { FilterSection } from "../../components/filters/FilterSection";
import { MaxCompaniesControl } from "../../components/filters/MaxCompaniesControl";
import { useDashboardFilters } from "../../hooks/useDashboardFilters";
import { createJob } from "../../services/jobs/jobApi";
import { appendSearchHistory } from "../../services/storage/settingsStorage";

export function DashboardPage() {
  const navigate = useNavigate();
  const filters = useDashboardFilters();
  const [error, setError] = useState<string | null>(null);

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

  return (
    <div className="flex flex-1 flex-col gap-4">
      <section className="space-y-1">
        <h1 className="font-display text-xl font-semibold tracking-tight text-mist-100">
          New scrape
        </h1>
        <p className="text-sm text-mist-300">
          Pick categories from the catalog, set volume, then choose country and city for local results.
        </p>
      </section>

      <section className="li-surface divide-y divide-white/10">
        <FilterSection
          step="1"
          title="Categories"
          hint="Required · searchable catalog"
          status={!filters.defaultLoaded ? "Loading" : undefined}
        >
          <CategorySelect
            selectedIds={filters.categoryIds}
            onToggle={filters.toggleCategory}
          />
        </FilterSection>

        <FilterSection step="2" title="Countries" hint="Optional · filters discovery by country">
          <CountryMultiSelect
            selectedCodes={filters.countryCodes}
            onToggle={filters.toggleCountry}
          />
        </FilterSection>

        <FilterSection
          step="3"
          title="Cities"
          hint="Recommended · each result is tagged with a city · empty expands to major cities in country"
        >
          <CityMultiSelect
            selectedIds={filters.cityIds}
            countryCodes={filters.countryCodes}
            onToggle={filters.toggleCity}
          />
        </FilterSection>

        <FilterSection
          step="4"
          title="Volume"
          hint="Required · max companies to process"
        >
          <MaxCompaniesControl
            value={filters.maxCompanies}
            onChange={filters.setMaxCompanies}
          />
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

      <p className="text-[11px] leading-relaxed text-mist-400">
        Jobs run asynchronously — you&apos;ll land on progress immediately after creation.
      </p>
    </div>
  );
}
