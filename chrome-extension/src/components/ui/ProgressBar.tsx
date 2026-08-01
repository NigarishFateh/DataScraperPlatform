type ProgressBarProps = {
  value: number;
  label?: string;
};

export function ProgressBar({ value, label }: ProgressBarProps) {
  const clamped = Math.max(0, Math.min(100, value));

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between gap-2 text-[11px]">
        <span className="text-mist-400">{label ?? "Progress"}</span>
        <span className="font-medium text-mist-200">{clamped}%</span>
      </div>
      <div
        className="h-2 overflow-hidden rounded-full bg-ink-900/80 ring-1 ring-white/10"
        role="progressbar"
        aria-valuenow={clamped}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div
          className="h-full rounded-full bg-signal transition-all duration-500 ease-out"
          style={{ width: `${clamped}%` }}
        />
      </div>
    </div>
  );
}
