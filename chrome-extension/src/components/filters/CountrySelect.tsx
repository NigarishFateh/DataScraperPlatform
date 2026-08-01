import { useInfiniteQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { fetchCountriesPage } from "../../services/catalog/catalogApi";
import type { Country } from "../../types/catalog";
import { SearchableMultiSelect, type SelectOption } from "../ui/SearchableMultiSelect";

type CountryMultiSelectProps = {
  selectedCodes: string[];
  onToggle: (countryCode: string) => void;
};

export function CountryMultiSelect({ selectedCodes, onToggle }: CountryMultiSelectProps) {
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);

  const query = useInfiniteQuery({
    queryKey: ["countries", debouncedSearch],
    queryFn: ({ pageParam }) =>
      fetchCountriesPage({ search: debouncedSearch, page: pageParam, pageSize: 50 }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? lastPage.page + 1 : undefined),
  });

  const countries = useMemo(
    () => query.data?.pages.flatMap((page) => page.items) ?? [],
    [query.data],
  );

  const selectedLookup = useMemo(() => {
    const map = new Map<string, Country>();
    for (const country of countries) {
      map.set(country.code, country);
    }
    return map;
  }, [countries]);

  const options: SelectOption[] = useMemo(() => {
    const fromQuery = countries.map((country) => ({
      id: country.code,
      label: country.name,
      sublabel: country.code,
    }));

    const missingSelected = selectedCodes
      .filter((code) => !selectedLookup.has(code))
      .map((code) => ({ id: code, label: code, sublabel: code }));

    return [...missingSelected, ...fromQuery];
  }, [countries, selectedCodes, selectedLookup]);

  return (
    <SearchableMultiSelect
      options={options}
      selectedIds={selectedCodes}
      search={search}
      placeholder="Search countries (optional)…"
      emptyMessage="No countries match"
      loading={query.isLoading}
      loadingMore={query.isFetchingNextPage}
      hasMore={Boolean(query.hasNextPage)}
      onSearchChange={setSearch}
      onToggle={onToggle}
      onLoadMore={() => {
        if (query.hasNextPage && !query.isFetchingNextPage) {
          void query.fetchNextPage();
        }
      }}
    />
  );
}
