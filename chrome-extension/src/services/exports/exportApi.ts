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
  const response = await apiFetch(`/api/exports/${exportId}/download`);
  if (!response.ok) {
    throw new Error(`Failed to download export (${response.status})`);
  }

  const blob = await response.blob();
  const disposition = response.headers.get("Content-Disposition") ?? "";
  const match = disposition.match(/filename="([^"]+)"/);
  const resolvedName = fileName ?? match?.[1] ?? `export-${exportId}.xlsx`;

  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = resolvedName;
  anchor.rel = "noopener";
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
