import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { stashPendingFilters } from "../../hooks/useDashboardFilters";
import { useAuth } from "../../hooks/useAuth";
import {
  clearSearchHistory,
  getSearchHistory,
} from "../../services/storage/settingsStorage";
import type { DashboardFilters, SearchHistoryEntry } from "../../types";

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return "?";
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function SettingsPage() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [history, setHistory] = useState<SearchHistoryEntry[]>([]);
  const [signingOut, setSigningOut] = useState(false);

  useEffect(() => {
    void getSearchHistory().then(setHistory);
  }, []);

  async function onClearHistory() {
    await clearSearchHistory();
    setHistory([]);
  }

  async function onSignOut() {
    setSigningOut(true);
    try {
      await logout();
      navigate("/auth", { replace: true });
    } finally {
      setSigningOut(false);
    }
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
        <p className="text-sm text-mist-300">Your profile and recent scrapes.</p>
      </section>

      <section className="li-surface space-y-4 p-4">
        <h2 className="text-sm font-semibold text-mist-100">Profile</h2>
        {user ? (
          <>
            <div className="flex items-center gap-3">
              {user.pictureUrl ? (
                <img
                  src={user.pictureUrl}
                  alt=""
                  className="h-12 w-12 shrink-0 rounded-full border border-white/10 object-cover"
                />
              ) : (
                <div
                  className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full border border-white/10 bg-signal/15 text-sm font-semibold text-signal"
                  aria-hidden
                >
                  {initials(user.displayName || user.email)}
                </div>
              )}
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-mist-100">{user.displayName}</p>
                <p className="truncate text-xs text-mist-400">{user.email}</p>
              </div>
            </div>
            <button
              type="button"
              className="li-btn-ghost w-full !py-2 text-xs text-red-300 hover:bg-red-500/10 hover:text-red-200"
              disabled={signingOut}
              onClick={() => void onSignOut()}
            >
              {signingOut ? "Signing out…" : "Sign out"}
            </button>
          </>
        ) : (
          <p className="text-xs text-mist-400">You are not signed in.</p>
        )}
      </section>

      <section className="li-surface space-y-3 p-4">
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-mist-100">Recent scrapes</h2>
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
          <p className="text-xs text-mist-400">Nothing here yet.</p>
        ) : (
          <ul className="space-y-2">
            {history.map((entry) => (
              <li
                key={entry.id}
                className="rounded-lg border border-white/10 bg-ink-900/40 px-3 py-2 text-xs"
              >
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
                    Run again
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
