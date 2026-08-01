import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { fetchCities } from "../../services/catalog/catalogApi";
import { SearchableMultiSelect, type SelectOption } from "../ui/SearchableMultiSelect";

type CityMultiSelectProps = {
  selectedIds: string[];
  onToggle: (cityId: string) => void;
};

export function CityMultiSelect({ selectedIds, onToggle }: CityMultiSelectProps) {
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);

  const query = useQuery({
    queryKey: ["cities", debouncedSearch],
    queryFn: () => fetchCities(debouncedSearch),
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

  return (
    <SearchableMultiSelect
      options={options}
      selectedIds={selectedIds}
      search={search}
      placeholder="Search cities (optional)…"
      emptyMessage="No cities match"
      loading={query.isLoading || query.isFetching}
      onSearchChange={setSearch}
      onToggle={onToggle}
    />
  );
}
