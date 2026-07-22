import { NavLink } from "react-router-dom";
import { BrandMark } from "./BrandMark";

const linkClass = ({ isActive }: { isActive: boolean }) =>
  [
    "rounded-md px-2.5 py-1.5 text-xs font-medium transition",
    isActive
      ? "bg-signal/15 text-signal"
      : "text-mist-300 hover:bg-white/5 hover:text-mist-100",
  ].join(" ");

export function TopBar() {
  return (
    <header className="sticky top-0 z-10 border-b border-white/10 bg-ink-900/85 px-4 py-3 backdrop-blur-md">
      <div className="flex items-center justify-between gap-3">
        <BrandMark compact />
        <nav className="flex items-center gap-1" aria-label="Primary">
          <NavLink to="/dashboard" className={linkClass}>
            Dashboard
          </NavLink>
          <NavLink to="/report" className={linkClass}>
            Report
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
