const PRESETS = [50, 100, 250, 500, 1000, 2000] as const;

type MaxCompaniesControlProps = {
  value: number | null;
  onChange: (value: number | null) => void;
};

export function MaxCompaniesControl({ value, onChange }: MaxCompaniesControlProps) {
  const sliderValue = value ?? 0;

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
      </div>

      <label className="block space-y-2">
        <div className="flex items-center justify-between gap-2 text-[11px]">
          <span className="text-mist-400">Drag to set volume</span>
          <span className={value ? "text-signal" : "text-mist-500"}>
            {value ? `${value.toLocaleString()} companies` : "Not set"}
          </span>
        </div>
        <input
          type="range"
          min={1}
          max={5000}
          step={1}
          value={sliderValue || 1}
          onChange={(event) => onChange(Number(event.target.value))}
          className="w-full accent-signal"
          aria-label="Max companies slider"
        />
      </label>

      <label className="block space-y-1">
        <span className="text-[11px] text-mist-400">Or type an exact number</span>
        <input
          type="number"
          min={1}
          max={5000}
          inputMode="numeric"
          placeholder="e.g. 150"
          value={value ?? ""}
          onChange={(event) => {
            const raw = event.target.value.trim();
            if (raw === "") {
              onChange(null);
              return;
            }
            const next = Number(raw);
            onChange(Number.isFinite(next) ? next : null);
          }}
          className="w-full rounded-lg border border-white/10 bg-ink-900/80 px-3 py-2.5 text-sm text-mist-100 outline-none placeholder:text-mist-500 focus:border-signal/50"
        />
      </label>
    </div>
  );
}
