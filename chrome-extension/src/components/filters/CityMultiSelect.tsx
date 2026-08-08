import { useQuery } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { fetchCities } from "../../services/catalog/catalogApi";
import { SearchableMultiSelect, type SelectOption } from "../ui/SearchableMultiSelect";

type CityMultiSelectProps = {
  selectedIds: string[];
  countryCodes: string[];
  onToggle: (cityId: string) => void;
};

export function CityMultiSelect({ selectedIds, countryCodes, onToggle }: CityMultiSelectProps) {
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);
  const hasCountries = countryCodes.length > 0;
  const countryKey = [...countryCodes].sort().join(",");

  useEffect(() => {
    setSearch("");
  }, [countryKey]);

  const query = useQuery({
    queryKey: ["cities", debouncedSearch, countryKey],
    queryFn: () => fetchCities(debouncedSearch, countryCodes),
    enabled: hasCountries,
  });

  const options: SelectOption[] = useMemo(
    () =>
      (query.data ?? []).map((city) => ({
        id: city.id,
        label: city.name,
        sublabel: city.countryCode,
      })),
    [query.data],
  );

  if (!hasCountries) {
    return (
      <p className="rounded-lg border border-dashed border-white/10 bg-ink-900/40 px-3 py-2.5 text-xs text-mist-400">
        Select a country first to see its cities. Leave cities empty to scrape nationwide
        (major cities first, then the rest of the country).
      </p>
    );
  }

  const total = options.length;
  const searching = debouncedSearch.trim().length > 0;

  return (
    <div className="space-y-2">
      <p className="text-[11px] text-mist-400">
        {query.isLoading || query.isFetching
          ? "Loading cities…"
          : searching
            ? `${total} match${total === 1 ? "" : "es"} in selected countries`
            : `${total} cit${total === 1 ? "y" : "ies"} available — leave empty for nationwide`}
      </p>
      <SearchableMultiSelect
        options={options}
        selectedIds={selectedIds}
        search={search}
        placeholder="Filter cities in selected countries…"
        emptyMessage="No cities found for the selected countries"
        loading={query.isLoading || query.isFetching}
        maxHeightClass="max-h-60"
        onSearchChange={setSearch}
        onToggle={onToggle}
      />
    </div>
  );
}
