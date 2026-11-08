import { NotificationBell } from "@/features/notifications";
import { DashboardShell } from "@/shared/layout/dashboard-shell";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return <DashboardShell headerActions={<NotificationBell />}>{children}</DashboardShell>;
}
