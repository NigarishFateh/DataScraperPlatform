import { HashRouter } from "react-router-dom";
import { AuthProvider } from "../hooks/useAuth";
import { AppRoutes } from "../routes/AppRoutes";
import { AppShell } from "../components/layout/AppShell";

/**
 * HashRouter is required in Chrome extensions.
 * There is no real server path — chrome-extension://.../sidepanel.html#/dashboard
 */
export function App() {
  return (
    <HashRouter>
      <AuthProvider>
        <AppShell>
          <AppRoutes />
        </AppShell>
      </AuthProvider>
    </HashRouter>
  );
}
