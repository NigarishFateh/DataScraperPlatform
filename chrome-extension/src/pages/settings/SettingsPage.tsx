import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { stashPendingFilters } from "../../hooks/useDashboardFilters";
import {
  clearSearchHistory,
  deleteSavedSearch,
  getSavedSearches,
  getSearchHistory,
  getUserSettings,
  saveUserSettings,
} from "../../services/storage/settingsStorage";
import type { DashboardFilters, SavedSearch, SearchHistoryEntry, UserSettings } from "../../types";

export function SettingsPage() {
  const navigate = useNavigate();
  const [savedSearches, setSavedSearches] = useState<SavedSearch[]>([]);
  const [history, setHistory] = useState<SearchHistoryEntry[]>([]);
  const [settings, setSettings] = useState<UserSettings | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    void (async () => {
      const [searches, entries, userSettings] = await Promise.all([
        getSavedSearches(),
        getSearchHistory(),
        getUserSettings(),
      ]);
      setSavedSearches(searches);
      setHistory(entries);
      setSettings(userSettings);
    })();
  }, []);

  async function onDeleteSaved(id: string) {
    await deleteSavedSearch(id);
    setSavedSearches(await getSavedSearches());
  }

  async function onClearHistory() {
    await clearSearchHistory();
    setHistory([]);
  }

  async function onSaveSettings() {
    if (!settings) return;
    await saveUserSettings(settings);
    setSaved(true);
    window.setTimeout(() => setSaved(false), 2000);
  }

  function loadFilters(filters: DashboardFilters) {
    stashPendingFilters(filters);
    navigate("/dashboard");
  }

  return (
    <div className="flex flex-1 flex-col gap-4">
      <section className="space-y-1">
        <h1 className="font-display text-xl font-semibold tracking-tight text-mist-100">
          Settings
        </h1>
        <p className="text-sm text-mist-300">
          Saved searches, history, and scraping defaults.
        </p>
      </section>

      <section className="li-surface space-y-3 p-4">
        <h2 className="text-sm font-semibold text-mist-100">Defaults</h2>
        {settings ? (
          <label className="block space-y-1 text-xs">
            <span className="text-mist-400">
              Default max companies (optional — leave blank to set on each scrape)
            </span>
            <input
              type="number"
              min={1}
              max={5000}
              placeholder="Not set"
              value={settings.defaultMaxCompanies ?? ""}
              onChange={(event) => {
                const raw = event.target.value.trim();
                setSettings({
                  ...settings,
                  defaultMaxCompanies: raw === "" ? null : Number(raw) || null,
                });
              }}
              className="w-full rounded-lg border border-white/10 bg-ink-900/80 px-3 py-2 text-sm text-mist-100 outline-none placeholder:text-mist-500 focus:border-signal/50"
            />
          </label>
        ) : null}
        <button
          type="button"
          className="li-btn-primary !py-2 text-xs"
          onClick={() => void onSaveSettings()}
        >
          {saved ? "Saved" : "Save settings"}
        </button>
      </section>

      <section className="li-surface space-y-3 p-4">
        <h2 className="text-sm font-semibold text-mist-100">Saved searches</h2>
        {savedSearches.length === 0 ? (
          <p className="text-xs text-mist-400">No saved searches yet.</p>
        ) : (
          <ul className="space-y-2">
            {savedSearches.map((search) => (
              <li
                key={search.id}
                className="flex items-center justify-between gap-2 rounded-lg border border-white/10 bg-ink-900/40 px-3 py-2"
              >
                <button
                  type="button"
                  className="min-w-0 flex-1 text-left"
                  onClick={() => loadFilters(search.filters)}
                >
                  <p className="truncate text-sm text-mist-100">{search.name}</p>
                  <p className="text-[10px] text-mist-500">
                    {search.filters.categoryIds.length} categories ·{" "}
                    {new Date(search.createdAt).toLocaleDateString()}
                  </p>
                </button>
                <button
                  type="button"
                  className="li-btn-ghost !px-2 !py-1 text-[10px]"
                  onClick={() => void onDeleteSaved(search.id)}
                >
                  Delete
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="li-surface space-y-3 p-4">
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-mist-100">Search history</h2>
          {history.length > 0 ? (
            <button
              type="button"
              className="li-btn-ghost !px-2 !py-1 text-[10px]"
              onClick={() => void onClearHistory()}
            >
              Clear
            </button>
          ) : null}
        </div>
        {history.length === 0 ? (
          <p className="text-xs text-mist-400">No recent searches.</p>
        ) : (
          <ul className="space-y-2">
            {history.map((entry) => (
              <li
                key={entry.id}
                className="rounded-lg border border-white/10 bg-ink-900/40 px-3 py-2 text-xs"
              >
                <p className="text-mist-300">
                  {entry.filters.categoryIds.length} categories ·{" "}
                  {entry.filters.countryCodes.length} countries ·{" "}
                  {entry.filters.cityIds.length} cities
                </p>
                <p className="text-[10px] text-mist-500">
                  {new Date(entry.createdAt).toLocaleString()}
                </p>
                <div className="mt-1 flex flex-wrap gap-3">
                  {entry.jobId ? (
                    <Link to={`/jobs/${entry.jobId}`} className="text-[10px] text-signal">
                      View job
                    </Link>
                  ) : null}
                  <button
                    type="button"
                    className="text-[10px] text-signal"
                    onClick={() => loadFilters(entry.filters)}
                  >
                    Reuse filters
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
