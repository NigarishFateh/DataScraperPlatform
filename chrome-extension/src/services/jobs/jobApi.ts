import { apiFetch } from "../api/client";
import type { PageResponse } from "../../types/common";
import type { CreateJobRequest, JobResponse } from "../../types/job";

async function parseJson<T>(response: Response, label: string): Promise<T> {
  if (!response.ok) {
    throw new Error(`Failed to ${label} (${response.status})`);
  }
  return (await response.json()) as T;
}

export async function createJob(request: CreateJobRequest): Promise<JobResponse> {
  const response = await apiFetch("/api/jobs", {
    method: "POST",
    body: JSON.stringify(request),
  });
  return parseJson(response, "create job");
}

export async function getJob(id: string): Promise<JobResponse> {
  const response = await apiFetch(`/api/jobs/${id}`);
  return parseJson(response, "load job");
}

export async function listJobs(page = 0, pageSize = 20): Promise<PageResponse<JobResponse>> {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  const response = await apiFetch(`/api/jobs?${params.toString()}`);
  return parseJson(response, "list jobs");
}

export async function cancelJob(id: string): Promise<JobResponse> {
  const response = await apiFetch(`/api/jobs/${id}/cancel`, { method: "POST" });
  return parseJson(response, "cancel job");
}

export async function resumeJob(id: string): Promise<JobResponse> {
  const response = await apiFetch(`/api/jobs/${id}/resume`, { method: "POST" });
  return parseJson(response, "resume job");
}

export async function retryJob(id: string): Promise<JobResponse> {
  const response = await apiFetch(`/api/jobs/${id}/retry`, { method: "POST" });
  return parseJson(response, "retry job");
}
