export const UNLIMITED_MAX_COMPANIES = -1;
/** UI hard cap for typed/slider volume (server still allows up to 100k via Unlimited). */
export const MAX_COMPANIES_LIMIT = 10_000;

const PRESETS = [50, 100, 250, 500, 1000, 2000, 5000, 10000] as const;

type MaxCompaniesControlProps = {
  value: number | null;
  onChange: (value: number | null) => void;
};

export function MaxCompaniesControl({ value, onChange }: MaxCompaniesControlProps) {
  const unlimited = value === UNLIMITED_MAX_COMPANIES;
  const sliderValue = unlimited || value == null ? 1 : value;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-1.5">
        {PRESETS.map((preset) => {
          const active = value === preset;
          return (
            <button
              key={preset}
              type="button"
              onClick={() => onChange(preset)}
              className={[
                "rounded-md px-2.5 py-1 text-xs font-medium transition",
                active
                  ? "bg-signal/20 text-signal ring-1 ring-signal/40"
                  : "bg-ink-900/80 text-mist-300 ring-1 ring-white/10 hover:bg-white/[0.06] hover:text-mist-100",
              ].join(" ")}
            >
              {preset.toLocaleString()}
            </button>
          );
        })}
        <button
          type="button"
          onClick={() => onChange(UNLIMITED_MAX_COMPANIES)}
          className={[
            "rounded-md px-2.5 py-1 text-xs font-medium transition",
            unlimited
              ? "bg-signal/20 text-signal ring-1 ring-signal/40"
              : "bg-ink-900/80 text-mist-300 ring-1 ring-white/10 hover:bg-white/[0.06] hover:text-mist-100",
          ].join(" ")}
        >
          Unlimited
        </button>
      </div>

      <label className="block space-y-2">
        <div className="flex items-center justify-between gap-2 text-[11px]">
          <span className="text-mist-400">Drag to set volume</span>
          <span className={value != null ? "text-signal" : "text-mist-500"}>
            {unlimited
              ? "Unlimited (fetches as many as available)"
              : value
                ? `${value.toLocaleString()} companies`
                : "Not set"}
          </span>
        </div>
        <input
          type="range"
          min={1}
          max={MAX_COMPANIES_LIMIT}
          step={1}
          disabled={unlimited}
          value={sliderValue}
          onChange={(event) => onChange(Number(event.target.value))}
          className="w-full accent-signal disabled:opacity-40"
          aria-label="Max companies slider"
        />
      </label>

      <label className="block space-y-1">
        <span className="text-[11px] text-mist-400">Or type an exact number</span>
        <input
          type="number"
          min={1}
          max={MAX_COMPANIES_LIMIT}
          inputMode="numeric"
          placeholder="e.g. 150"
          disabled={unlimited}
          value={unlimited || value == null ? "" : value}
          onChange={(event) => {
            const raw = event.target.value.trim();
            if (raw === "") {
              onChange(null);
              return;
            }
            const next = Number(raw);
            onChange(Number.isFinite(next) ? next : null);
          }}
          className="w-full rounded-lg border border-white/10 bg-ink-900/80 px-3 py-2.5 text-sm text-mist-100 outline-none placeholder:text-mist-500 focus:border-signal/50 disabled:opacity-40"
        />
      </label>
    </div>
  );
}
