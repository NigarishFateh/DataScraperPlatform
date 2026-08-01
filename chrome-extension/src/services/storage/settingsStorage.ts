import type {
  DashboardFilters,
  SavedSearch,
  SearchHistoryEntry,
  UserSettings,
} from "../../types";
import { DEFAULT_USER_SETTINGS } from "../../types/settings";

const STORAGE_KEYS = {
  savedSearches: "gbi.savedSearches",
  searchHistory: "gbi.searchHistory",
  settings: "gbi.settings",
} as const;

const MAX_SEARCH_HISTORY = 20;

async function readStorage<T>(key: string, fallback: T): Promise<T> {
  const result = await chrome.storage.local.get(key);
  const value = result[key];
  return (value as T | undefined) ?? fallback;
}

async function writeStorage<T>(key: string, value: T): Promise<void> {
  await chrome.storage.local.set({ [key]: value });
}

export async function getSavedSearches(): Promise<SavedSearch[]> {
  return readStorage(STORAGE_KEYS.savedSearches, []);
}

export async function saveSavedSearch(search: SavedSearch): Promise<void> {
  const existing = await getSavedSearches();
  const next = [search, ...existing.filter((item) => item.id !== search.id)].slice(0, 50);
  await writeStorage(STORAGE_KEYS.savedSearches, next);
}

export async function deleteSavedSearch(id: string): Promise<void> {
  const existing = await getSavedSearches();
  await writeStorage(
    STORAGE_KEYS.savedSearches,
    existing.filter((item) => item.id !== id),
  );
}

export async function getSearchHistory(): Promise<SearchHistoryEntry[]> {
  return readStorage(STORAGE_KEYS.searchHistory, []);
}

export async function appendSearchHistory(
  filters: DashboardFilters,
  jobId: string | null,
): Promise<void> {
  const existing = await getSearchHistory();
  const entry: SearchHistoryEntry = {
    id: crypto.randomUUID(),
    filters,
    jobId,
    createdAt: new Date().toISOString(),
  };
  const next = [entry, ...existing].slice(0, MAX_SEARCH_HISTORY);
  await writeStorage(STORAGE_KEYS.searchHistory, next);
}

export async function clearSearchHistory(): Promise<void> {
  await writeStorage(STORAGE_KEYS.searchHistory, []);
}

export async function getUserSettings(): Promise<UserSettings> {
  return readStorage(STORAGE_KEYS.settings, DEFAULT_USER_SETTINGS);
}

export async function saveUserSettings(settings: UserSettings): Promise<void> {
  await writeStorage(STORAGE_KEYS.settings, settings);
}

export function createSavedSearch(name: string, filters: DashboardFilters): SavedSearch {
  return {
    id: crypto.randomUUID(),
    name,
    filters,
    createdAt: new Date().toISOString(),
  };
}
