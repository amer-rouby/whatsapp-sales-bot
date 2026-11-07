"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useI18n } from "@/shared/i18n/provider";
import { cn } from "@/shared/lib/cn";
import { activeNavItem, NAV_ITEMS } from "./nav-items";

export function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  const { t } = useI18n();
  const active = activeNavItem(pathname);

  return (
    <nav className="flex flex-col gap-1" aria-label={t.app.name}>
      {NAV_ITEMS.map((item) => {
        const isActive = item === active;
        const Icon = item.icon;
        return (
          <Link
            key={item.href}
            href={item.href}
            onClick={onNavigate}
            aria-current={isActive ? "page" : undefined}
            className={cn(
              "group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold transition",
              isActive ? "bg-white/15 text-white" : "text-brand-100 hover:bg-white/10 hover:text-white",
            )}
          >
            {/* gold marker on the active item, on the inner edge (flips with RTL/LTR) */}
            {isActive && <span className="absolute inset-y-2 start-0 w-1 rounded-full bg-gold-500" aria-hidden />}
            <Icon size={18} strokeWidth={2} className={isActive ? "text-gold-300" : "opacity-80"} aria-hidden />
            {t.nav[item.label]}
          </Link>
        );
      })}
    </nav>
  );
}
