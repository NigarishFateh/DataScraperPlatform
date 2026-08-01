import { useMutation, useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { downloadExport, formatFileSize, listExports } from "../../services/exports/exportApi";

export function ExportsPage() {
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ["export-center"],
    queryFn: () => listExports(),
  });

  const rows = useMemo(() => query.data ?? [], [query.data]);

  const downloadMutation = useMutation({
    mutationFn: ({ exportId, fileName }: { exportId: string; fileName?: string }) =>
      downloadExport(exportId, fileName),
    onMutate: ({ exportId }) => {
      setDownloadingId(exportId);
      setError(null);
    },
    onError: (err) => setError(err instanceof Error ? err.message : "Download failed"),
    onSettled: () => setDownloadingId(null),
  });

  return (
    <div className="flex flex-1 flex-col gap-4">
      <section className="space-y-1">
        <h1 className="font-display text-xl font-semibold tracking-tight text-mist-100">
          Download center
        </h1>
        <p className="text-sm text-mist-300">Exports from completed scraping jobs.</p>
      </section>

      {query.isLoading ? (
        <p className="text-sm text-mist-400">Loading exports…</p>
      ) : query.isError ? (
        <p className="text-sm text-red-300">Failed to load exports.</p>
      ) : rows.length === 0 ? (
        <div className="li-surface p-4 text-sm text-mist-300">
          No exports yet. Complete a job to generate a download.{" "}
          <Link to="/dashboard" className="text-signal">
            Start scraping
          </Link>
        </div>
      ) : (
        <ul className="space-y-2">
          {rows.map((row) => (
            <li key={`${row.jobId}-${row.id}`} className="li-surface p-3">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0 space-y-1">
                  <p className="truncate text-sm font-medium text-mist-100">
                    {row.fileName ?? `Export ${row.id.slice(0, 8)}`}
                  </p>
                  <p className="truncate font-mono text-[11px] text-mist-400">
                    Job {row.jobId}
                  </p>
                  <p className="text-[11px] text-mist-500">
                    {new Date(row.createdAt).toLocaleString()}
                    {row.fileSizeBytes > 0 ? ` · ${formatFileSize(row.fileSizeBytes)}` : ""}
                    {row.rowCount > 0 ? ` · ${row.rowCount} rows` : ""}
                  </p>
                </div>
                <StatusBadge status={row.status} kind="export" />
              </div>

              {row.status === "READY" ? (
                <button
                  type="button"
                  className="li-btn-primary mt-3 !py-2 text-xs"
                  disabled={downloadingId === row.id}
                  onClick={() =>
                    downloadMutation.mutate({
                      exportId: row.id,
                      fileName: row.fileName ?? undefined,
                    })
                  }
                >
                  {downloadingId === row.id ? "Downloading…" : "Download"}
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      )}

      {error ? <p className="text-[11px] text-red-300">{error}</p> : null}
    </div>
  );
}
