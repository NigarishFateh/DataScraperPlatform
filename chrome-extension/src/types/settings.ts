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
  defaultMaxCompanies: number;
};

export const DEFAULT_USER_SETTINGS: UserSettings = {
  defaultMaxCompanies: 200,
};
