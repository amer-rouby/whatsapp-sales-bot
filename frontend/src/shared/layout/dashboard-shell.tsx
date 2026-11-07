import { SidebarNav } from "./sidebar-nav";

const VERSION = process.env.NEXT_PUBLIC_APP_VERSION;

/** Sidebar on desktop, top bar on mobile. Uses logical sides so it flips with dir="rtl"/"ltr". */
export function DashboardShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col md:flex-row">
      <aside className="flex flex-col border-b border-border bg-surface p-4 md:w-60 md:shrink-0 md:border-b-0 md:border-e">
        <div className="mb-4 text-xl font-extrabold text-primary">بياع</div>
        <SidebarNav />
        {VERSION && (
          <p className="mt-auto hidden pt-6 text-xs text-muted md:block" dir="ltr">
            v{VERSION}
          </p>
        )}
      </aside>
      <main className="flex-1 p-4 md:p-8">{children}</main>
    </div>
  );
}
