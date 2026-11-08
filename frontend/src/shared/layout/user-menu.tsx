"use client";

import { LogOut, UserRound } from "lucide-react";
import { logout } from "@/shared/auth/actions";
import type { SessionUser } from "@/shared/auth/constants";
import { useI18n } from "@/shared/i18n/provider";

/** Signed-in user's name + sign out (a Server Action clears the httpOnly cookie). */
export function UserMenu({ user }: { user: SessionUser }) {
  const { t } = useI18n();
  return (
    <div className="flex items-center gap-1">
      <span className="hidden max-w-40 items-center gap-2 truncate px-2 text-sm font-bold lg:inline-flex" title={user.email}>
        <UserRound size={16} className="shrink-0 text-gold-500" aria-hidden />
        <span dir="auto" className="truncate">{user.name}</span>
      </span>
      <form action={logout}>
        <button
          type="submit"
          className="inline-flex h-10 items-center gap-2 rounded-xl px-2 text-sm font-bold text-white transition hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-gold-500 sm:px-3"
          aria-label={t.header.signOut}
          title={t.header.signOut}
        >
          <LogOut size={18} className="rtl:-scale-x-100" aria-hidden />
          <span className="max-sm:sr-only">{t.header.signOut}</span>
        </button>
      </form>
    </div>
  );
}
