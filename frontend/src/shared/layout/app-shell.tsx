"use client";

import { Menu, Sparkles, X } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { useI18n } from "@/shared/i18n/provider";
import { cn } from "@/shared/lib/cn";
import type { Theme } from "@/shared/preferences/theme";
import { activeNavItem } from "./nav-items";
import { LanguageSwitcher, ThemeSwitcher } from "./preference-switchers";
import { SidebarNav } from "./sidebar-nav";

const VERSION = process.env.NEXT_PUBLIC_APP_VERSION;

/**
 * The app frame (same layout language as the Tawasol web app):
 * brand-colored sidebar (drawer on mobile) + brand-colored header + page area.
 */
export function AppShell({ theme, children }: { theme: Theme; children: React.ReactNode }) {
  const { t } = useI18n();
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  const current = activeNavItem(pathname);

  return (
    <div className="flex min-h-screen">
      {menuOpen && (
        <div
          className="fixed inset-0 z-30 bg-brand-900/50 backdrop-blur-[2px] lg:hidden"
          onClick={() => setMenuOpen(false)}
          aria-hidden
        />
      )}

      <aside
        className={cn(
          "fixed inset-y-0 start-0 z-40 flex w-72 shrink-0 flex-col bg-brand-700 text-white shadow-2xl transition-transform duration-200",
          "lg:static lg:shadow-none",
          // hidden off the start edge on small screens only; always visible from lg up
          !menuOpen && "max-lg:ltr:-translate-x-full max-lg:rtl:translate-x-full",
        )}
      >
        <div className="flex justify-end p-2 lg:hidden">
          <button
            type="button"
            className="grid size-9 place-items-center rounded-lg text-white/80 hover:bg-white/10"
            onClick={() => setMenuOpen(false)}
            aria-label={t.header.closeMenu}
          >
            <X size={18} />
          </button>
        </div>

        <Link href="/" className="flex items-center gap-3 border-b border-white/10 px-6 pb-6 pt-4 lg:pt-8">
          <span className="grid size-12 place-items-center rounded-2xl bg-gold-500 text-brand-900 shadow-lg">
            <Sparkles size={24} strokeWidth={2.2} aria-hidden />
          </span>
          <span>
            <span className="block text-2xl font-extrabold leading-tight">{t.app.name}</span>
            <span className="block text-xs text-brand-200">{t.app.tagline}</span>
          </span>
        </Link>

        <div className="flex-1 overflow-y-auto px-3 py-5">
          <SidebarNav onNavigate={() => setMenuOpen(false)} />
        </div>

        {VERSION && (
          <p className="border-t border-white/10 px-6 py-3 text-xs text-brand-200">
            {t.app.version} <span data-ltr>v{VERSION}</span>
          </p>
        )}
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 flex items-center gap-1.5 bg-brand-700 px-2 py-2.5 text-white shadow-md sm:gap-3 sm:px-6">
          <button
            type="button"
            className="grid size-10 shrink-0 place-items-center rounded-xl transition hover:bg-white/10 lg:hidden"
            onClick={() => setMenuOpen(true)}
            aria-label={t.header.openMenu}
          >
            <Menu size={20} />
          </button>

          <h1 className="min-w-0 flex-1 truncate text-base font-bold sm:text-lg">
            {current ? t.nav[current.label] : t.app.name}
          </h1>

          <span className="hidden items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-xs font-bold md:inline-flex">
            <span className="relative flex size-2">
              <span className="absolute inline-flex size-full animate-ping rounded-full bg-emerald-400 opacity-75" />
              <span className="relative inline-flex size-2 rounded-full bg-emerald-400" />
            </span>
            {t.header.botOnline}
          </span>
          <ThemeSwitcher current={theme} />
          <LanguageSwitcher />
        </header>

        <main className="mx-auto w-full max-w-7xl flex-1 p-4 sm:p-6 lg:p-8">{children}</main>
      </div>
    </div>
  );
}
