import { getSessionUser } from "@/shared/auth/session";
import { getPreferences } from "@/shared/preferences/server";
import { AppShell } from "./app-shell";

/** Server entry for the dashboard frame: reads the theme and the signed-in user once for the client shell. */
/** `headerActions`: feature widgets for the header (e.g. the notification bell); shared/ can't import features. */
export async function DashboardShell({ children, headerActions }: { children: React.ReactNode; headerActions?: React.ReactNode }) {
  const [{ theme }, user] = await Promise.all([getPreferences(), getSessionUser()]);
  return (
    <AppShell theme={theme} user={user} headerActions={headerActions}>
      {children}
    </AppShell>
  );
}
