import type { Category } from "../../types/catalog";

type CategorySelectProps = {
  categories: Category[];
  selectedIds: string[];
  loading?: boolean;
  locked?: boolean;
  onToggle: (categoryId: string) => void;
};

export function CategorySelect({
  categories,
  selectedIds,
  loading,
  locked,
  onToggle,
}: CategorySelectProps) {
  if (locked) {
    return (
      <div className="rounded-lg border border-dashed border-white/10 bg-ink-900/50 px-3 py-2.5 text-sm text-mist-400">
        Select companies first
      </div>
    );
  }

  if (loading) {
    return <p className="text-xs text-mist-400">Loading categories…</p>;
  }

  if (categories.length === 0) {
    return <p className="text-xs text-mist-400">No categories for the selected companies</p>;
  }

  return (
    <div className="space-y-2">
      <div className="flex max-h-40 flex-wrap gap-1.5 overflow-y-auto rounded-lg border border-white/10 bg-ink-900/40 p-2">
        {categories.map((category) => {
          const active = selectedIds.includes(category.id);
          return (
            <button
              key={category.id}
              type="button"
              onClick={() => onToggle(category.id)}
              className={[
                "rounded-md px-2.5 py-1 text-[11px] font-medium transition",
                active
                  ? "bg-signal/20 text-signal ring-1 ring-signal/40"
                  : "bg-white/5 text-mist-300 hover:bg-white/10 hover:text-mist-100",
              ].join(" ")}
            >
              {category.name}
            </button>
          );
        })}
      </div>
      {selectedIds.length > 0 ? (
        <p className="text-[11px] text-signal">{selectedIds.length} selected</p>
      ) : null}
    </div>
  );
}
