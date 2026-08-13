import { useMutation, useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { JobPhaseIndicator } from "../../components/jobs/JobPhaseIndicator";
import { ProgressBar } from "../../components/ui/ProgressBar";
import { Spinner } from "../../components/ui/Spinner";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { downloadExport, listExportsByJob } from "../../services/exports/exportApi";
import {
  cancelJob,
  getJob,
  resumeJob,
  retryJob,
} from "../../services/jobs/jobApi";
import { isTerminalJobStatus, type JobPhase, type JobStatus } from "../../types/job";

function isDiscoveryOrEnrichmentActive(phase: JobPhase, status: JobStatus): boolean {
  return (
    (status === "RUNNING" || status === "QUEUED") &&
    (phase === "DISCOVERY" || phase === "ENRICHMENT")
  );
}

function formatEta(seconds: number | null): string {
  if (seconds == null || seconds <= 0) return "—";
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return remainder > 0 ? `${minutes}m ${remainder}s` : `${minutes}m`;
}

function formatTimestamp(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toLocaleString();
}

export function JobProgressPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [actionError, setActionError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(false);

  const jobQuery = useQuery({
    queryKey: ["job", id],
    queryFn: () => getJob(id!),
    enabled: Boolean(id),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status && isTerminalJobStatus(status) ? false : 2000;
    },
  });

  const job = jobQuery.data;
  const needsExportPoll = Boolean(
    job &&
      !job.exportId &&
      (job.status === "COMPLETED" ||
        ((job.status === "FAILED" || job.status === "CANCELLED") && job.persistedCount > 0)),
  );

  const exportsQuery = useQuery({
    queryKey: ["exports", id],
    queryFn: () => listExportsByJob(id!),
    enabled: Boolean(id && needsExportPoll),
    refetchInterval: needsExportPoll ? 2000 : false,
  });

  const readyExport = useMemo(() => {
    if (job?.exportId) {
      return { id: job.exportId, fileName: null };
    }
    const exports = exportsQuery.data ?? [];
    const ready = exports.find((item) => item.status === "READY");
    return ready ? { id: ready.id, fileName: ready.fileName } : null;
  }, [exportsQuery.data, job?.exportId]);

  const cancelMutation = useMutation({
    mutationFn: () => cancelJob(id!),
    onSuccess: () => {
      setActionError(null);
      void jobQuery.refetch();
    },
    onError: (err) => setActionError(err instanceof Error ? err.message : "Cancel failed"),
  });

  const resumeMutation = useMutation({
    mutationFn: () => resumeJob(id!),
    onSuccess: () => {
      setActionError(null);
      void jobQuery.refetch();
    },
    onError: (err) => setActionError(err instanceof Error ? err.message : "Resume failed"),
  });

  const retryMutation = useMutation({
    mutationFn: () => retryJob(id!),
    onSuccess: (nextJob) => {
      setActionError(null);
      navigate(`/jobs/${nextJob.id}`, { replace: true });
    },
    onError: (err) => setActionError(err instanceof Error ? err.message : "Retry failed"),
  });

  async function onDownload() {
    if (!readyExport) return;
    setDownloading(true);
    setActionError(null);
    try {
      await downloadExport(readyExport.id, readyExport.fileName ?? undefined);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Download failed");
    } finally {
      setDownloading(false);
    }
  }

  if (!id) {
    return (
      <div className="li-surface p-4 text-sm text-mist-300">
        Missing job id. <Link to="/jobs" className="text-signal">View jobs</Link>
      </div>
    );
  }

  if (jobQuery.isLoading) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-3 text-sm text-mist-300">
        <Spinner size="lg" />
        <p>Loading job…</p>
      </div>
    );
  }

  if (jobQuery.isError || !job) {
    return (
      <div className="li-surface space-y-2 p-4 text-sm text-mist-300">
        <p className="font-display font-semibold text-mist-100">Job not found</p>
        <Link to="/jobs" className="text-signal text-xs">
          Back to job history
        </Link>
      </div>
    );
  }

  const showCancel = !isTerminalJobStatus(job.status) && job.status !== "PAUSED";
  const showResume = job.status === "PAUSED";
  const showRetry = job.status === "FAILED";
  const showDownload = Boolean(readyExport) &&
    (job.status === "COMPLETED" || job.status === "FAILED" || job.status === "CANCELLED");
  const partialDownload = showDownload && job.status !== "COMPLETED";
  const showPhaseAnimation = isDiscoveryOrEnrichmentActive(job.phase, job.status);

  return (
    <div className="flex flex-1 flex-col gap-4">
      <section className="space-y-1">
        <div className="flex items-center gap-2">
          <h1 className="font-display text-xl font-semibold tracking-tight text-mist-100">
            Job progress
          </h1>
          <StatusBadge status={job.status} />
        </div>
        <p className="truncate font-mono text-[11px] text-mist-400">{job.id}</p>
      </section>

      <section className="li-surface space-y-4 p-4">
        <JobPhaseIndicator
          phase={job.phase}
          status={job.status}
          discoveredCount={job.discoveredCount}
          enrichedCount={job.enrichedCount}
        />

        <div className="grid grid-cols-2 gap-2">
          <div
            className={`rounded-lg border px-3 py-3 text-center transition-colors ${
              job.phase === "DISCOVERY" && (job.status === "RUNNING" || job.status === "QUEUED")
                ? "border-signal/40 bg-signal/10"
                : "border-white/10 bg-ink-900/40"
            }`}
          >
            <p className="text-[10px] uppercase tracking-wide text-mist-400">Discovered</p>
            <p
              className={`mt-1 font-display text-2xl font-semibold tabular-nums ${
                job.phase === "DISCOVERY" && job.status === "RUNNING"
                  ? "animate-pulse text-signal"
                  : "text-mist-100"
              }`}
            >
              {job.discoveredCount.toLocaleString()}
            </p>
            <p className="text-[10px] text-mist-500">companies found</p>
          </div>
          <div
            className={`rounded-lg border px-3 py-3 text-center transition-colors ${
              job.phase === "ENRICHMENT" && job.status === "RUNNING"
                ? "border-signal/40 bg-signal/10"
                : "border-white/10 bg-ink-900/40"
            }`}
          >
            <p className="text-[10px] uppercase tracking-wide text-mist-400">Enriched</p>
            <p
              className={`mt-1 font-display text-2xl font-semibold tabular-nums ${
                job.phase === "ENRICHMENT" && job.status === "RUNNING"
                  ? "animate-pulse text-signal"
                  : "text-mist-100"
              }`}
            >
              {job.enrichedCount.toLocaleString()}
            </p>
            <p className="text-[10px] text-mist-500">details collected</p>
          </div>
        </div>

        <ProgressBar
          value={job.progressPercent}
          indeterminate={showPhaseAnimation}
        />

        <dl className="grid grid-cols-2 gap-3 text-xs">
          <div>
            <dt className="text-mist-500">Phase</dt>
            <dd className="flex items-center gap-1.5 font-medium text-mist-100">
              {showPhaseAnimation ? <Spinner size="sm" /> : null}
              {job.phase.replace(/_/g, " ")}
            </dd>
          </div>
          <div>
            <dt className="text-mist-500">ETA</dt>
            <dd className="font-medium text-mist-100">
              {formatEta(job.estimatedRemainingSeconds)}
            </dd>
          </div>
          <div>
            <dt className="text-mist-500">Persisted</dt>
            <dd className="font-medium text-mist-100">{job.persistedCount}</dd>
          </div>
          <div>
            <dt className="text-mist-500">Failed</dt>
            <dd className="font-medium text-mist-100">{job.failedCount}</dd>
          </div>
        </dl>

        {job.errorMessage ? (
          <p className="rounded-lg border border-red-400/20 bg-red-400/10 px-3 py-2 text-xs text-red-200">
            {job.errorMessage}
          </p>
        ) : null}

        <div className="flex flex-wrap gap-2">
          {showCancel ? (
            <button
              type="button"
              className="li-btn-ghost text-xs"
              disabled={cancelMutation.isPending}
              onClick={() => cancelMutation.mutate()}
            >
              {cancelMutation.isPending ? "Cancelling…" : "Cancel"}
            </button>
          ) : null}
          {showResume ? (
            <button
              type="button"
              className="li-btn-primary !w-auto !px-4 text-xs"
              disabled={resumeMutation.isPending}
              onClick={() => resumeMutation.mutate()}
            >
              {resumeMutation.isPending ? "Resuming…" : "Resume"}
            </button>
          ) : null}
          {showRetry ? (
            <button
              type="button"
              className="li-btn-primary !w-auto !px-4 text-xs"
              disabled={retryMutation.isPending}
              onClick={() => retryMutation.mutate()}
            >
              {retryMutation.isPending ? "Retrying…" : "Retry"}
            </button>
          ) : null}
          {showDownload ? (
            <button
              type="button"
              className="li-btn-primary !w-auto !px-4 text-xs"
              disabled={downloading}
              onClick={() => void onDownload()}
            >
              {downloading
                ? "Downloading…"
                : partialDownload
                  ? "Download partial Excel"
                  : "Download export"}
            </button>
          ) : null}
          {(job.status === "COMPLETED" ||
            ((job.status === "FAILED" || job.status === "CANCELLED") && job.persistedCount > 0)) &&
          !readyExport ? (
            <span className="flex items-center gap-1.5 self-center text-[11px] text-mist-400">
              <Spinner size="sm" />
              Preparing export…
            </span>
          ) : null}
        </div>

        {actionError ? <p className="text-[11px] text-red-300">{actionError}</p> : null}
      </section>

      <section className="li-surface space-y-2 p-4 text-xs text-mist-400">
        <p>Created {formatTimestamp(job.createdAt)}</p>
        {job.startedAt ? <p>Started {formatTimestamp(job.startedAt)}</p> : null}
        {job.completedAt ? <p>Completed {formatTimestamp(job.completedAt)}</p> : null}
      </section>
    </div>
  );
}
