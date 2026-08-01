import { NavLink, useNavigate } from "react-router-dom";
import { BrandMark } from "./BrandMark";
import { useAuth } from "../../hooks/useAuth";

const linkClass = ({ isActive }: { isActive: boolean }) =>
  [
    "rounded-md px-2.5 py-1.5 text-xs font-medium transition",
    isActive
      ? "bg-signal/15 text-signal"
      : "text-mist-300 hover:bg-white/5 hover:text-mist-100",
  ].join(" ");

export function TopBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function onLogout() {
    await logout();
    navigate("/auth", { replace: true });
  }

  return (
    <header className="sticky top-0 z-10 border-b border-white/10 bg-ink-900/85 px-4 py-3 backdrop-blur-md">
      <div className="flex items-center justify-between gap-3">
        <BrandMark compact />
        <div className="flex items-center gap-2">
          <nav className="flex items-center gap-1" aria-label="Primary">
            <NavLink to="/dashboard" className={linkClass}>
              Dashboard
            </NavLink>
            <NavLink to="/jobs" className={linkClass}>
              Jobs
            </NavLink>
            <NavLink to="/exports" className={linkClass}>
              Exports
            </NavLink>
            <NavLink to="/settings" className={linkClass}>
              Settings
            </NavLink>
          </nav>
          <button
            type="button"
            className="li-btn-ghost !px-2 !py-1 text-[11px]"
            title={user?.email}
            onClick={() => void onLogout()}
          >
            Log out
          </button>
        </div>
      </div>
    </header>
  );
}
