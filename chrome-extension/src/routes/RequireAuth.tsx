import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

/** Route guard — unauthenticated users bounce to /auth. */
export function RequireAuth() {
  const { user, bootstrapping } = useAuth();

  if (bootstrapping) {
    return (
      <div className="flex flex-1 items-center justify-center text-sm text-mist-300">
        Restoring session…
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/auth" replace />;
  }

  return <Outlet />;
}
