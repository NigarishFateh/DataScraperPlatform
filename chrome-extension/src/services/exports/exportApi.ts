import { apiFetch } from "../api/client";
import type { ExportResponse } from "../../types/export";

async function parseJson<T>(response: Response, label: string): Promise<T> {
  if (!response.ok) {
    throw new Error(`Failed to ${label} (${response.status})`);
  }
  return (await response.json()) as T;
}

export async function listExports(jobId?: string): Promise<ExportResponse[]> {
  const params = new URLSearchParams();
  if (jobId?.trim()) {
    params.set("jobId", jobId.trim());
  }
  const query = params.toString();
  const response = await apiFetch(`/api/exports${query ? `?${query}` : ""}`);
  return parseJson(response, "list exports");
}

export async function listExportsByJob(jobId: string): Promise<ExportResponse[]> {
  return listExports(jobId);
}

export async function getExport(id: string): Promise<ExportResponse> {
  const response = await apiFetch(`/api/exports/${id}`);
  return parseJson(response, "load export");
}

export async function downloadExport(exportId: string, fileName?: string): Promise<void> {
  const response = await apiFetch(`/api/exports/${exportId}/download`, {
    method: "GET",
    headers: { Accept: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
  });

  if (!response.ok) {
    let detail = `Failed to download export (${response.status})`;
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) {
        detail = body.message;
      }
    } catch {
      // keep status message
    }
    throw new Error(detail);
  }

  const blob = await response.blob();
  if (blob.size === 0) {
    throw new Error("Download returned an empty file");
  }

  const disposition = response.headers.get("Content-Disposition") ?? "";
  const match = disposition.match(/filename\*?=(?:UTF-8''|")?([^\";]+)"?/i);
  const headerName = match?.[1] ? decodeURIComponent(match[1].replace(/"/g, "")) : null;
  const resolvedName = sanitizeDownloadName(fileName ?? headerName ?? `export-${exportId}.xlsx`);

  const url = URL.createObjectURL(blob);

  try {
    if (typeof chrome !== "undefined" && chrome.downloads?.download) {
      await new Promise<void>((resolve, reject) => {
        chrome.downloads.download(
          {
            url,
            filename: resolvedName,
            saveAs: true,
            conflictAction: "uniquify",
          },
          (downloadId) => {
            const err = chrome.runtime.lastError;
            if (err || downloadId === undefined) {
              reject(new Error(err?.message ?? "Chrome download failed"));
              return;
            }
            resolve();
          },
        );
      });
    } else {
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = resolvedName;
      anchor.rel = "noopener";
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
    }
  } finally {
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }
}

function sanitizeDownloadName(name: string): string {
  const cleaned = name.trim().replace(/[\\/:*?"<>|]+/g, "-");
  return cleaned.toLowerCase().endsWith(".xlsx") ? cleaned : `${cleaned}.xlsx`;
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
