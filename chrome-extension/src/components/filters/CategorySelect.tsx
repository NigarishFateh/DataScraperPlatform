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
      fetchCategoriesPage({ search: debouncedSearch, page: pageParam, pageSize: 100 }),
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

  const totalHint = query.data?.pages[0]?.total;

  return (
    <div className="space-y-2">
      <p className="text-[11px] leading-relaxed text-mist-400">
        Search and select from the full category catalog
        {typeof totalHint === "number" ? ` (${totalHint.toLocaleString()} industries)` : ""}.
        Scroll for more.
      </p>
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
        maxHeightClass="max-h-64"
        onSearchChange={setSearch}
        onToggle={onToggle}
        onLoadMore={() => {
          if (query.hasNextPage && !query.isFetchingNextPage) {
            void query.fetchNextPage();
          }
        }}
      />
    </div>
  );
}
