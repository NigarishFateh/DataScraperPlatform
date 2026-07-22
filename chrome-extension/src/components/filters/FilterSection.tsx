import type { ReactNode } from "react";

type FilterSectionProps = {
  step: string;
  title: string;
  hint: string;
  locked?: boolean;
  status?: string;
  children: ReactNode;
};

export function FilterSection({
  step,
  title,
  hint,
  locked,
  status,
  children,
}: FilterSectionProps) {
  return (
    <div className={`space-y-2 p-3.5 ${locked ? "opacity-55" : ""}`}>
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="grid h-5 w-5 place-items-center rounded-md bg-signal/15 font-display text-[10px] font-semibold text-signal">
            {step}
          </span>
          <h2 className="text-sm font-semibold text-mist-100">{title}</h2>
        </div>
        {status ? (
          <span className="text-[10px] uppercase tracking-wide text-mist-400">{status}</span>
        ) : null}
      </div>
      <p className="text-[11px] text-mist-400">{hint}</p>
      {children}
    </div>
  );
}
