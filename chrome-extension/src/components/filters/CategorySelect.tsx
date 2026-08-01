import { useInfiniteQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { fetchCategoriesPage } from "../../services/catalog/catalogApi";
import { SearchableMultiSelect, type SelectOption } from "../ui/SearchableMultiSelect";

type CategorySelectProps = {
  selectedIds: string[];
  onToggle: (categoryId: string) => void;
};

export function CategorySelect({ selectedIds, onToggle }: CategorySelectProps) {
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);

  const query = useInfiniteQuery({
    queryKey: ["categories", debouncedSearch],
    queryFn: ({ pageParam }) =>
      fetchCategoriesPage({ search: debouncedSearch, page: pageParam, pageSize: 50 }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? lastPage.page + 1 : undefined),
  });

  const categories = useMemo(
    () => query.data?.pages.flatMap((page) => page.items) ?? [],
    [query.data],
  );

  const options: SelectOption[] = useMemo(
    () =>
      categories.map((category) => ({
        id: category.id,
        label: category.name,
      })),
    [categories],
  );

  return (
    <SearchableMultiSelect
      options={options}
      selectedIds={selectedIds}
      search={search}
      placeholder="Search categories…"
      emptyMessage="No categories match"
      loading={query.isLoading}
      loadingMore={query.isFetchingNextPage}
      hasMore={Boolean(query.hasNextPage)}
      required
      maxHeightClass="max-h-52"
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
