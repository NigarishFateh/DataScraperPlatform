type SpinnerProps = {
  size?: "sm" | "md" | "lg";
  className?: string;
};

const SIZE_CLASSES = {
  sm: "h-3.5 w-3.5 border",
  md: "h-5 w-5 border-2",
  lg: "h-7 w-7 border-2",
} as const;

export function Spinner({ size = "md", className = "" }: SpinnerProps) {
  return (
    <span
      className={`inline-block shrink-0 animate-spin rounded-full border-signal/25 border-t-signal ${SIZE_CLASSES[size]} ${className}`}
      role="status"
      aria-label="Loading"
    />
  );
}
