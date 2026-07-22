import { useNavigate } from "react-router-dom";

/**
 * Screen 2 — Dashboard shell.
 * Phase 5 adds real Country → City → Company → Category selectors + dummy data.
 */
export function DashboardPage() {
  const navigate = useNavigate();

  return (
    <div className="flex flex-1 flex-col gap-4">
      <section className="space-y-1">
        <h1 className="font-display text-xl font-semibold tracking-tight text-mist-100">
          Intelligence search
        </h1>
        <p className="text-sm text-mist-300">
          Filter European IT companies, then run an enrichment job.
        </p>
      </section>

      <section className="li-surface divide-y divide-white/10">
        <FilterSlot
          step="1"
          title="Country"
          hint="European countries first"
          placeholder="Select country"
        />
        <FilterSlot
          step="2"
          title="City"
          hint="Loads after country · multi-select · searchable"
          placeholder="Select cities"
          locked
        />
        <FilterSlot
          step="3"
          title="Companies"
          hint="Search · pagination · infinite scroll"
          placeholder="Select companies"
          locked
        />
        <FilterSlot
          step="4"
          title="Categories"
          hint="Depends on selected companies"
          placeholder="Select categories"
          locked
        />
      </section>

      <button
        type="button"
        className="li-btn-primary"
        onClick={() => navigate("/report")}
      >
        Search
      </button>

      <p className="text-[11px] leading-relaxed text-mist-400">
        Phase 3 layout only. Filters and backend jobs arrive in later phases.
      </p>
    </div>
  );
}

type FilterSlotProps = {
  step: string;
  title: string;
  hint: string;
  placeholder: string;
  locked?: boolean;
};

function FilterSlot({ step, title, hint, placeholder, locked }: FilterSlotProps) {
  return (
    <div className={`space-y-2 p-3.5 ${locked ? "opacity-55" : ""}`}>
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="grid h-5 w-5 place-items-center rounded-md bg-signal/15 font-display text-[10px] font-semibold text-signal">
            {step}
          </span>
          <h2 className="text-sm font-semibold text-mist-100">{title}</h2>
        </div>
        {locked ? (
          <span className="text-[10px] uppercase tracking-wide text-mist-400">Waiting</span>
        ) : null}
      </div>
      <p className="text-[11px] text-mist-400">{hint}</p>
      <div className="rounded-lg border border-dashed border-white/12 bg-ink-900/50 px-3 py-2.5 text-sm text-mist-400">
        {placeholder}
      </div>
    </div>
  );
}
