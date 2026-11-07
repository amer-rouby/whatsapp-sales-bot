import { getPreferences } from "@/shared/preferences/server";
import { AppShell } from "./app-shell";

/** Server entry for the dashboard frame: reads the theme once and hands it to the client shell. */
export async function DashboardShell({ children }: { children: React.ReactNode }) {
  const { theme } = await getPreferences();
  return <AppShell theme={theme}>{children}</AppShell>;
}
