import { getSessionUser } from "@/shared/auth/session";
import { getPreferences } from "@/shared/preferences/server";
import { AppShell } from "./app-shell";

/** Server entry for the dashboard frame: reads the theme and the signed-in user once for the client shell. */
export async function DashboardShell({ children }: { children: React.ReactNode }) {
  const [{ theme }, user] = await Promise.all([getPreferences(), getSessionUser()]);
  return (
    <AppShell theme={theme} user={user}>
      {children}
    </AppShell>
  );
}
