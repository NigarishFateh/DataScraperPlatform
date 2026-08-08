import { Navigate, Route, Routes } from "react-router-dom";
import { AuthPage } from "../pages/auth/AuthPage";
import { CustomScrapePage } from "../pages/custom/CustomScrapePage";
import { DashboardPage } from "../pages/dashboard/DashboardPage";
import { ExportsPage } from "../pages/exports/ExportsPage";
import { JobHistoryPage } from "../pages/jobs/JobHistoryPage";
import { JobProgressPage } from "../pages/jobs/JobProgressPage";
import { SettingsPage } from "../pages/settings/SettingsPage";
import { RequireAuth } from "./RequireAuth";
import { useAuth } from "../hooks/useAuth";

export function AppRoutes() {
  const { user, bootstrapping } = useAuth();

  if (bootstrapping) {
    return (
      <div className="flex flex-1 items-center justify-center text-sm text-mist-300">
        Starting Global BI…
      </div>
    );
  }

  return (
    <Routes>
      <Route
        path="/auth"
        element={user ? <Navigate to="/dashboard" replace /> : <AuthPage />}
      />
      <Route element={<RequireAuth />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/custom" element={<CustomScrapePage />} />
        <Route path="/jobs" element={<JobHistoryPage />} />
        <Route path="/jobs/:id" element={<JobProgressPage />} />
        <Route path="/exports" element={<ExportsPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/report" element={<Navigate to="/jobs" replace />} />
      </Route>
      <Route path="/" element={<Navigate to={user ? "/dashboard" : "/auth"} replace />} />
      <Route path="*" element={<Navigate to={user ? "/dashboard" : "/auth"} replace />} />
    </Routes>
  );
}
