type BrandMarkProps = {
  compact?: boolean;
};

export function BrandMark({ compact = false }: BrandMarkProps) {
  return (
    <div className="flex items-center gap-2.5">
      <div
        className="grid h-8 w-8 place-items-center rounded-lg bg-signal/15 ring-1 ring-signal/30"
        aria-hidden
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
          <path
            d="M4 16.5L9.2 8.8L13.1 13.2L20 4"
            stroke="#3dcea7"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <circle cx="20" cy="4" r="1.6" fill="#3dcea7" />
        </svg>
      </div>
      <div className="min-w-0">
        <p className="font-display text-sm font-semibold tracking-tight text-mist-100">
          Global BI
        </p>
        {!compact ? (
          <p className="truncate text-[11px] text-mist-400">Business Intelligence Platform</p>
        ) : null}
      </div>
    </div>
  );
}
