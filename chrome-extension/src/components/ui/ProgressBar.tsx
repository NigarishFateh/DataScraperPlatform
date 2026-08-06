type ProgressBarProps = {
  value: number;
  label?: string;
  indeterminate?: boolean;
};

export function ProgressBar({ value, label, indeterminate = false }: ProgressBarProps) {
  const clamped = Math.max(0, Math.min(100, value));

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between gap-2 text-[11px]">
        <span className="text-mist-400">{label ?? "Progress"}</span>
        <span className="font-medium text-mist-200">{indeterminate ? "…" : `${clamped}%`}</span>
      </div>
      <div
        className="relative h-2 overflow-hidden rounded-full bg-ink-900/80 ring-1 ring-white/10"
        role="progressbar"
        aria-valuenow={indeterminate ? undefined : clamped}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        {indeterminate ? (
          <>
            <div className="absolute inset-y-0 left-0 w-1/3 rounded-full bg-signal/70 animate-[progress-indeterminate_1.4s_ease-in-out_infinite]" />
            <div className="absolute inset-y-0 left-0 w-1/4 rounded-full bg-signal/35 animate-[progress-indeterminate_1.4s_ease-in-out_infinite] [animation-delay:200ms]" />
          </>
        ) : (
          <div
            className="h-full rounded-full bg-signal transition-all duration-500 ease-out"
            style={{ width: `${clamped}%` }}
          />
        )}
      </div>
    </div>
  );
}
