import type { JobStatus } from "../../types/job";
import type { ExportStatus } from "../../types/export";

const JOB_STATUS_STYLES: Record<JobStatus, string> = {
  QUEUED: "bg-mist-400/15 text-mist-300 ring-mist-400/30",
  RUNNING: "bg-signal/15 text-signal ring-signal/30",
  PAUSED: "bg-amber-400/15 text-amber-200 ring-amber-400/30",
  COMPLETED: "bg-emerald-400/15 text-emerald-300 ring-emerald-400/30",
  FAILED: "bg-red-400/15 text-red-300 ring-red-400/30",
  CANCELLED: "bg-mist-400/10 text-mist-400 ring-mist-400/20",
};

const EXPORT_STATUS_STYLES: Record<ExportStatus, string> = {
  PENDING: "bg-mist-400/15 text-mist-300 ring-mist-400/30",
  GENERATING: "bg-signal/15 text-signal ring-signal/30",
  READY: "bg-emerald-400/15 text-emerald-300 ring-emerald-400/30",
  FAILED: "bg-red-400/15 text-red-300 ring-red-400/30",
  EXPIRED: "bg-mist-400/10 text-mist-400 ring-mist-400/20",
};

type StatusBadgeProps = {
  status: JobStatus | ExportStatus;
  kind?: "job" | "export";
};

export function StatusBadge({ status, kind = "job" }: StatusBadgeProps) {
  const styles =
    kind === "export"
      ? EXPORT_STATUS_STYLES[status as ExportStatus]
      : JOB_STATUS_STYLES[status as JobStatus];

  return (
    <span
      className={[
        "inline-flex items-center rounded-md px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ring-1 ring-inset",
        styles,
      ].join(" ")}
    >
      {status.replace(/_/g, " ")}
    </span>
  );
}
