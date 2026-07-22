import { useEffect, useRef } from "react";
import type { Company } from "../../types/catalog";

type CompanySelectProps = {
  companies: Company[];
  selectedIds: string[];
  search: string;
  total: number;
  loading?: boolean;
  loadingMore?: boolean;
  hasMore?: boolean;
  locked?: boolean;
  onSearchChange: (value: string) => void;
  onToggle: (companyId: string) => void;
  onLoadMore: () => void;
};

export function CompanySelect({
  companies,
  selectedIds,
  search,
  total,
  loading,
  loadingMore,
  hasMore,
  locked,
  onSearchChange,
  onToggle,
  onLoadMore,
}: CompanySelectProps) {
  const sentinelRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (locked || !hasMore || loadingMore) return;
    const node = sentinelRef.current;
    if (!node) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          onLoadMore();
        }
      },
      { root: node.parentElement, rootMargin: "40px" },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [hasMore, loadingMore, locked, onLoadMore, companies.length]);

  if (locked) {
    return (
      <div className="rounded-lg border border-dashed border-white/10 bg-ink-900/50 px-3 py-2.5 text-sm text-mist-400">
        Select one or more cities first
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <input
        type="search"
        value={search}
        onChange={(event) => onSearchChange(event.target.value)}
        placeholder="Search name, website, industry…"
        className="w-full rounded-lg border border-white/10 bg-ink-900/80 px-3 py-2 text-sm text-mist-100 outline-none placeholder:text-mist-400 focus:border-signal/50"
      />
      <div className="max-h-48 overflow-y-auto rounded-lg border border-white/10 bg-ink-900/40">
        {loading && companies.length === 0 ? (
          <p className="px-3 py-3 text-xs text-mist-400">Loading companies…</p>
        ) : companies.length === 0 ? (
          <p className="px-3 py-3 text-xs text-mist-400">No companies match</p>
        ) : (
          <ul className="divide-y divide-white/5">
            {companies.map((company) => {
              const checked = selectedIds.includes(company.id);
              return (
                <li key={company.id}>
                  <label className="flex cursor-pointer items-start gap-2.5 px-3 py-2 hover:bg-white/[0.03]">
                    <input
                      type="checkbox"
                      className="mt-0.5 rounded border-white/20 bg-ink-800 text-signal focus:ring-signal"
                      checked={checked}
                      onChange={() => onToggle(company.id)}
                    />
                    <span className="min-w-0">
                      <span className="block truncate text-sm text-mist-100">{company.name}</span>
                      <span className="block truncate text-[11px] text-mist-400">
                        {company.website.replace(/^https?:\/\//, "")} · {company.industry}
                      </span>
                    </span>
                  </label>
                </li>
              );
            })}
          </ul>
        )}
        <div ref={sentinelRef} className="h-3" />
        {loadingMore ? (
          <p className="px-3 pb-2 text-[11px] text-mist-400">Loading more…</p>
        ) : null}
      </div>
      <p className="text-[11px] text-mist-400">
        Showing {companies.length} of {total}
        {selectedIds.length > 0 ? ` · ${selectedIds.length} selected` : ""}
      </p>
    </div>
  );
}
