import { useNavigate } from "react-router-dom";
import { CategorySelect } from "../../components/filters/CategorySelect";
import { CityMultiSelect } from "../../components/filters/CityMultiSelect";
import { CompanySelect } from "../../components/filters/CompanySelect";
import { CountrySelect } from "../../components/filters/CountrySelect";
import { FilterSection } from "../../components/filters/FilterSection";
import { useDashboardFilters } from "../../hooks/useDashboardFilters";
import type { Company, DashboardSelection } from "../../types/catalog";
import { COUNTRIES } from "../../data/dummyCatalog";

export const SEARCH_SELECTION_KEY = "li.lastSearchSelection";

export type SearchPayload = DashboardSelection & {
  companies?: Company[];
};

/**
 * Screen 2 — Dashboard with cascading filters (dummy catalog).
 * Backend Location/Company/Category services replace catalogApi in later phases.
 */
export function DashboardPage() {
  const navigate = useNavigate();
  const filters = useDashboardFilters();

  function onSearch() {
    if (!filters.canSearch) return;
    const payload: SearchPayload = {
      ...filters.selection,
      companies: filters.companies.filter((company) =>
        filters.selection.companyIds.includes(company.id),
      ),
    };
    sessionStorage.setItem(SEARCH_SELECTION_KEY, JSON.stringify(payload));
    navigate("/report", { state: payload });
  }

  const countryName =
    COUNTRIES.find((country) => country.code === filters.countryCode)?.name ?? null;

  return (
    <div className="flex flex-1 flex-col gap-4">
      <section className="space-y-1">
        <h1 className="font-display text-xl font-semibold tracking-tight text-mist-100">
          Intelligence search
        </h1>
        <p className="text-sm text-mist-300">
          Filter European IT companies, then run an enrichment job.
        </p>
      </section>

      <section className="li-surface divide-y divide-white/10">
        <FilterSection
          step="1"
          title="Country"
          hint="European countries first"
          status={filters.countriesQuery.isLoading ? "Loading" : undefined}
        >
          <CountrySelect
            countries={filters.countriesQuery.data ?? []}
            value={filters.countryCode}
            loading={filters.countriesQuery.isLoading}
            onChange={filters.setCountryCode}
          />
        </FilterSection>

        <FilterSection
          step="2"
          title="City"
          hint="Loads after country · multi-select · searchable"
          locked={!filters.countryCode}
          status={
            !filters.countryCode
              ? "Waiting"
              : filters.citiesQuery.isFetching
                ? "Loading"
                : undefined
          }
        >
          <CityMultiSelect
            cities={filters.citiesQuery.data ?? []}
            selectedIds={filters.cityIds}
            search={filters.citySearch}
            loading={filters.citiesQuery.isFetching}
            locked={!filters.countryCode}
            onSearchChange={filters.setCitySearch}
            onToggle={filters.toggleCity}
          />
        </FilterSection>

        <FilterSection
          step="3"
          title="Companies"
          hint="Search · pagination · infinite scroll"
          locked={filters.cityIds.length === 0}
          status={
            filters.cityIds.length === 0
              ? "Waiting"
              : filters.companiesQuery.isFetching && filters.companies.length === 0
                ? "Loading"
                : undefined
          }
        >
          <CompanySelect
            companies={filters.companies}
            selectedIds={filters.companyIds}
            search={filters.companySearch}
            total={filters.totalCompanies}
            loading={filters.companiesQuery.isFetching && filters.companies.length === 0}
            loadingMore={filters.companiesQuery.isFetchingNextPage}
            hasMore={Boolean(filters.companiesQuery.hasNextPage)}
            locked={filters.cityIds.length === 0}
            onSearchChange={filters.setCompanySearch}
            onToggle={filters.toggleCompany}
            onLoadMore={() => {
              if (filters.companiesQuery.hasNextPage && !filters.companiesQuery.isFetchingNextPage) {
                void filters.companiesQuery.fetchNextPage();
              }
            }}
          />
        </FilterSection>

        <FilterSection
          step="4"
          title="Categories"
          hint="Depends on selected companies"
          locked={filters.companyIds.length === 0}
          status={
            filters.companyIds.length === 0
              ? "Waiting"
              : filters.categoriesQuery.isFetching
                ? "Loading"
                : undefined
          }
        >
          <CategorySelect
            categories={filters.categoriesQuery.data ?? []}
            selectedIds={filters.categoryIds}
            loading={filters.categoriesQuery.isFetching}
            locked={filters.companyIds.length === 0}
            onToggle={filters.toggleCategory}
          />
        </FilterSection>
      </section>

      <button
        type="button"
        className="li-btn-primary disabled:cursor-not-allowed disabled:opacity-50"
        disabled={!filters.canSearch}
        onClick={onSearch}
      >
        Search
      </button>

      <p className="text-[11px] leading-relaxed text-mist-400">
        {countryName
          ? `Location + Company APIs · ${countryName} · categories from selected companies`
          : "Countries/cities/companies from backend when services are running."}
      </p>
    </div>
  );
}
