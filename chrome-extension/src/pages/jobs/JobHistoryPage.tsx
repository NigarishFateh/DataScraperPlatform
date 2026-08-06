import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Spinner } from "../../components/ui/Spinner";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { listJobs } from "../../services/jobs/jobApi";
import type { JobPhase, JobStatus } from "../../types/job";

function formatWhen(value: string): string {
  return new Date(value).toLocaleString();
}

function phaseActivityLabel(phase: JobPhase, status: JobStatus): string | null {
  if (status !== "RUNNING" && status !== "QUEUED") {
    return null;
  }
  if (phase === "DISCOVERY") return "Discovering companies";
  if (phase === "ENRICHMENT") return "Enriching details";
  return null;
}

export function JobHistoryPage() {
  const [page, setPage] = useState(0);
  const pageSize = 20;

  const query = useQuery({
    queryKey: ["jobs", page, pageSize],
    queryFn: () => listJobs(page, pageSize),
  });

  const jobs = query.data?.items ?? [];

  return (
    <div className="flex flex-1 flex-col gap-4">
      <section className="space-y-1">
        <h1 className="font-display text-xl font-semibold tracking-tight text-mist-100">
          Job history
        </h1>
        <p className="text-sm text-mist-300">Recent scraping jobs and their status.</p>
      </section>

      {query.isLoading ? (
        <div className="flex items-center gap-2 text-sm text-mist-400">
          <Spinner size="sm" />
          Loading jobs…
        </div>
      ) : query.isError ? (
        <p className="text-sm text-red-300">Failed to load jobs.</p>
      ) : jobs.length === 0 ? (
        <div className="li-surface p-4 text-sm text-mist-300">
          No jobs yet.{" "}
          <Link to="/dashboard" className="text-signal">
            Start scraping
          </Link>
        </div>
      ) : (
        <ul className="space-y-2">
          {jobs.map((job) => {
            const activity = phaseActivityLabel(job.phase, job.status);

            return (
            <li key={job.id}>
              <Link
                to={`/jobs/${job.id}`}
                className="li-surface block p-3 transition hover:border-signal/30 hover:bg-ink-800/90"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0 space-y-1">
                    <p className="truncate font-mono text-[11px] text-mist-400">{job.id}</p>
                    <p className="text-xs text-mist-300">
                      {job.categoryIds.length} categories · {job.progressPercent}% ·{" "}
                      {formatWhen(job.createdAt)}
                    </p>
                    {activity ? (
                      <p className="flex items-center gap-1.5 text-[11px] text-signal">
                        <Spinner size="sm" />
                        {activity}
                      </p>
                    ) : null}
                  </div>
                  <StatusBadge status={job.status} />
                </div>
              </Link>
            </li>
            );
          })}
        </ul>
      )}

      {query.data ? (
        <div className="flex items-center justify-between gap-2 text-xs">
          <button
            type="button"
            className="li-btn-ghost !px-3 !py-1.5"
            disabled={page === 0}
            onClick={() => setPage((prev) => Math.max(0, prev - 1))}
          >
            Previous
          </button>
          <span className="text-mist-400">
            Page {query.data.page + 1} · {query.data.total} total
          </span>
          <button
            type="button"
            className="li-btn-ghost !px-3 !py-1.5"
            disabled={!query.data.hasMore}
            onClick={() => setPage((prev) => prev + 1)}
          >
            Next
          </button>
        </div>
      ) : null}
    </div>
  );
}
