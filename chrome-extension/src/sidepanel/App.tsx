import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { HashRouter } from "react-router-dom";
import { AuthProvider } from "../hooks/useAuth";
import { AppRoutes } from "../routes/AppRoutes";
import { AppShell } from "../components/layout/AppShell";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

/**
 * HashRouter is required in Chrome extensions.
 * QueryClientProvider = server-state cache for catalog filters (Phase 5).
 */
export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <HashRouter>
        <AuthProvider>
          <AppShell>
            <AppRoutes />
          </AppShell>
        </AuthProvider>
      </HashRouter>
    </QueryClientProvider>
  );
}
