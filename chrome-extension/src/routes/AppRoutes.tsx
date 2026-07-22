import { Navigate, Route, Routes } from "react-router-dom";
import { AuthPage } from "../pages/auth/AuthPage";
import { DashboardPage } from "../pages/dashboard/DashboardPage";
import { ReportPage } from "../pages/report/ReportPage";
import { RequireAuth } from "./RequireAuth";
import { useAuth } from "../hooks/useAuth";

export function AppRoutes() {
  const { user, bootstrapping } = useAuth();

  if (bootstrapping) {
    return (
      <div className="flex flex-1 items-center justify-center text-sm text-mist-300">
        Starting Lead Intelligence…
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
        <Route path="/report" element={<ReportPage />} />
      </Route>
      <Route path="/" element={<Navigate to={user ? "/dashboard" : "/auth"} replace />} />
      <Route path="*" element={<Navigate to={user ? "/dashboard" : "/auth"} replace />} />
    </Routes>
  );
}
