import type { DashboardFilters } from "./catalog";

export type SavedSearch = {
  id: string;
  name: string;
  filters: DashboardFilters;
  createdAt: string;
};

export type SearchHistoryEntry = {
  id: string;
  filters: DashboardFilters;
  jobId: string | null;
  createdAt: string;
};

export type UserSettings = {
  /** Optional dashboard pre-fill; null/0 means user must set volume on New scrape. */
  defaultMaxCompanies: number | null;
};

export const DEFAULT_USER_SETTINGS: UserSettings = {
  defaultMaxCompanies: null,
};
