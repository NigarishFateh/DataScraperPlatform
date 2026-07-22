import type { ReactNode } from "react";
import { useLocation } from "react-router-dom";
import { TopBar } from "./TopBar";

type AppShellProps = {
  children: ReactNode;
};

export function AppShell({ children }: AppShellProps) {
  const { pathname } = useLocation();
  const showChrome = pathname !== "/auth";

  return (
    <div className="li-shell flex min-h-full flex-col">
      {showChrome ? <TopBar /> : null}
      <main className="flex flex-1 flex-col px-4 pb-5 pt-4">{children}</main>
    </div>
  );
}
