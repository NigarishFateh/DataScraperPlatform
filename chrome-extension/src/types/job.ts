export type JobStatus =
  | "QUEUED"
  | "RUNNING"
  | "PAUSED"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED";

export type JobPhase =
  | "CREATED"
  | "DISCOVERY"
  | "ENRICHMENT"
  | "AGGREGATION"
  | "NORMALIZATION"
  | "VALIDATION"
  | "PERSISTENCE"
  | "EXPORT"
  | "DONE";

export type CreateJobRequest = {
  categoryIds: string[];
  countryCodes?: string[];
  cityIds?: string[];
  maxCompanies?: number;
  /** Preferred: named-company scrape list (Custom page). */
  companyNames?: string[];
  /** Backward-compatible mirror of companyNames. */
  options?: {
    companyNames?: string[];
  };
};

export type JobResponse = {
  id: string;
  status: JobStatus;
  phase: JobPhase;
  userId: string;
  categoryIds: string[];
  countryCodes: string[];
  cityIds: string[];
  discoveredCount: number;
  enrichedCount: number;
  persistedCount: number;
  failedCount: number;
  progressPercent: number;
  estimatedRemainingSeconds: number | null;
  exportId: string | null;
  errorMessage: string | null;
  checkpoint: string | null;
  createdAt: string;
  updatedAt: string;
  startedAt: string | null;
  completedAt: string | null;
};

export const TERMINAL_JOB_STATUSES: JobStatus[] = ["COMPLETED", "FAILED", "CANCELLED"];

export function isTerminalJobStatus(status: JobStatus): boolean {
  return TERMINAL_JOB_STATUSES.includes(status);
}
