import type { City } from "../../types/catalog";

type CityMultiSelectProps = {
  cities: City[];
  selectedIds: string[];
  search: string;
  loading?: boolean;
  locked?: boolean;
  onSearchChange: (value: string) => void;
  onToggle: (cityId: string) => void;
};

export function CityMultiSelect({
  cities,
  selectedIds,
  search,
  loading,
  locked,
  onSearchChange,
  onToggle,
}: CityMultiSelectProps) {
  if (locked) {
    return <LockedPlaceholder text="Select a country first" />;
  }

  return (
    <div className="space-y-2">
      <input
        type="search"
        value={search}
        onChange={(event) => onSearchChange(event.target.value)}
        placeholder="Search cities…"
        className="w-full rounded-lg border border-white/10 bg-ink-900/80 px-3 py-2 text-sm text-mist-100 outline-none placeholder:text-mist-400 focus:border-signal/50"
      />
      <div className="max-h-40 overflow-y-auto rounded-lg border border-white/10 bg-ink-900/40">
        {loading ? (
          <p className="px-3 py-3 text-xs text-mist-400">Loading cities…</p>
        ) : cities.length === 0 ? (
          <p className="px-3 py-3 text-xs text-mist-400">No cities match</p>
        ) : (
          <ul className="divide-y divide-white/5">
            {cities.map((city) => {
              const checked = selectedIds.includes(city.id);
              return (
                <li key={city.id}>
                  <label className="flex cursor-pointer items-center gap-2.5 px-3 py-2 text-sm hover:bg-white/[0.03]">
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => onToggle(city.id)}
                      className="rounded border-white/20 bg-ink-800 text-signal focus:ring-signal"
                    />
                    <span className="text-mist-100">{city.name}</span>
                  </label>
                </li>
              );
            })}
          </ul>
        )}
      </div>
      {selectedIds.length > 0 ? (
        <p className="text-[11px] text-signal">{selectedIds.length} selected</p>
      ) : null}
    </div>
  );
}

function LockedPlaceholder({ text }: { text: string }) {
  return (
    <div className="rounded-lg border border-dashed border-white/10 bg-ink-900/50 px-3 py-2.5 text-sm text-mist-400">
      {text}
    </div>
  );
}
