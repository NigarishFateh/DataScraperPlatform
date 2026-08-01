import { useCallback, useEffect, useId, useMemo, useRef, useState } from "react";

export type SelectOption = {
  id: string;
  label: string;
  sublabel?: string;
};

type SearchableMultiSelectProps = {
  options: SelectOption[];
  selectedIds: string[];
  search: string;
  placeholder?: string;
  emptyMessage?: string;
  loading?: boolean;
  loadingMore?: boolean;
  hasMore?: boolean;
  required?: boolean;
  maxHeightClass?: string;
  onSearchChange: (value: string) => void;
  onToggle: (id: string) => void;
  onLoadMore?: () => void;
};

const ROW_HEIGHT = 36;
const OVERSCAN = 4;

export function SearchableMultiSelect({
  options,
  selectedIds,
  search,
  placeholder = "Search…",
  emptyMessage = "No matches",
  loading,
  loadingMore,
  hasMore,
  required,
  maxHeightClass = "max-h-44",
  onSearchChange,
  onToggle,
  onLoadMore,
}: SearchableMultiSelectProps) {
  const listId = useId();
  const listRef = useRef<HTMLDivElement>(null);
  const [focusedIndex, setFocusedIndex] = useState(0);
  const [scrollTop, setScrollTop] = useState(0);
  const [viewportHeight, setViewportHeight] = useState(176);

  const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);

  const visibleRange = useMemo(() => {
    const start = Math.max(0, Math.floor(scrollTop / ROW_HEIGHT) - OVERSCAN);
    const visibleCount = Math.ceil(viewportHeight / ROW_HEIGHT) + OVERSCAN * 2;
    const end = Math.min(options.length, start + visibleCount);
    return { start, end };
  }, [options.length, scrollTop, viewportHeight]);

  const virtualItems = useMemo(
    () => options.slice(visibleRange.start, visibleRange.end),
    [options, visibleRange.end, visibleRange.start],
  );

  const onScroll = useCallback(() => {
    const node = listRef.current;
    if (!node) return;
    setScrollTop(node.scrollTop);
    setViewportHeight(node.clientHeight);

    if (hasMore && onLoadMore && !loadingMore) {
      const nearBottom = node.scrollTop + node.clientHeight >= node.scrollHeight - ROW_HEIGHT * 2;
      if (nearBottom) {
        onLoadMore();
      }
    }
  }, [hasMore, loadingMore, onLoadMore]);

  useEffect(() => {
    setFocusedIndex((prev) => Math.min(prev, Math.max(0, options.length - 1)));
  }, [options.length]);

  function moveFocus(delta: number) {
    if (options.length === 0) return;
    setFocusedIndex((prev) => {
      const next = Math.max(0, Math.min(options.length - 1, prev + delta));
      const node = listRef.current;
      if (node) {
        const rowTop = next * ROW_HEIGHT;
        const rowBottom = rowTop + ROW_HEIGHT;
        if (rowTop < node.scrollTop) {
          node.scrollTop = rowTop;
        } else if (rowBottom > node.scrollTop + node.clientHeight) {
          node.scrollTop = rowBottom - node.clientHeight;
        }
      }
      return next;
    });
  }

  function onSearchKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      moveFocus(1);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      moveFocus(-1);
    } else if (event.key === "Enter" && options[focusedIndex]) {
      event.preventDefault();
      onToggle(options[focusedIndex].id);
    }
  }

  function onOptionKeyDown(event: React.KeyboardEvent<HTMLButtonElement>, index: number) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setFocusedIndex(Math.min(options.length - 1, index + 1));
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setFocusedIndex(Math.max(0, index - 1));
    } else if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onToggle(options[index].id);
    }
  }

  return (
    <div className="space-y-2">
      <input
        type="search"
        value={search}
        onChange={(event) => onSearchChange(event.target.value)}
        onKeyDown={onSearchKeyDown}
        placeholder={placeholder}
        aria-controls={listId}
        className="w-full rounded-lg border border-white/10 bg-ink-900/80 px-3 py-2 text-sm text-mist-100 outline-none placeholder:text-mist-400 focus:border-signal/50"
      />

      <div
        ref={listRef}
        id={listId}
        role="listbox"
        aria-multiselectable="true"
        onScroll={onScroll}
        className={[
          "overflow-y-auto rounded-lg border border-white/10 bg-ink-900/40",
          maxHeightClass,
        ].join(" ")}
      >
        {loading && options.length === 0 ? (
          <p className="px-3 py-3 text-xs text-mist-400">Loading…</p>
        ) : options.length === 0 ? (
          <p className="px-3 py-3 text-xs text-mist-400">{emptyMessage}</p>
        ) : (
          <div style={{ height: options.length * ROW_HEIGHT, position: "relative" }}>
            {virtualItems.map((option, offset) => {
              const index = visibleRange.start + offset;
              const active = selectedSet.has(option.id);
              const focused = index === focusedIndex;

              return (
                <button
                  key={option.id}
                  type="button"
                  role="option"
                  aria-selected={active}
                  tabIndex={focused ? 0 : -1}
                  onKeyDown={(event) => onOptionKeyDown(event, index)}
                  onClick={() => onToggle(option.id)}
                  onMouseEnter={() => setFocusedIndex(index)}
                  className={[
                    "absolute inset-x-0 flex w-full items-center gap-2.5 px-3 text-left text-sm transition",
                    focused ? "bg-white/[0.06]" : "hover:bg-white/[0.03]",
                  ].join(" ")}
                  style={{ top: index * ROW_HEIGHT, height: ROW_HEIGHT }}
                >
                  <span
                    className={[
                      "grid h-4 w-4 shrink-0 place-items-center rounded border text-[10px]",
                      active
                        ? "border-signal bg-signal/20 text-signal"
                        : "border-white/20 bg-ink-800 text-transparent",
                    ].join(" ")}
                    aria-hidden
                  >
                    ✓
                  </span>
                  <span className="min-w-0 truncate text-mist-100">{option.label}</span>
                  {option.sublabel ? (
                    <span className="ml-auto shrink-0 text-[10px] text-mist-500">
                      {option.sublabel}
                    </span>
                  ) : null}
                </button>
              );
            })}
          </div>
        )}

        {loadingMore ? (
          <p className="border-t border-white/5 px-3 py-2 text-[10px] text-mist-400">
            Loading more…
          </p>
        ) : null}
      </div>

      <div className="flex items-center justify-between gap-2 text-[11px]">
        <span className={selectedIds.length > 0 ? "text-signal" : "text-mist-400"}>
          {selectedIds.length} selected
          {required && selectedIds.length === 0 ? " · required" : ""}
        </span>
        {hasMore && !loadingMore ? (
          <span className="text-mist-500">Scroll for more</span>
        ) : null}
      </div>
    </div>
  );
}
