"use client";

import { Bot, FlaskConical } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useI18n } from "@/shared/i18n/provider";
import { cn } from "@/shared/lib/cn";
import { formatChatTime } from "@/shared/lib/format";
import type { ConversationSummary } from "../types";

function initials(name: string) {
  return name.trim().slice(0, 1).toUpperCase();
}

/** WhatsApp-like chat list. Client only to know which chat is open (highlight + hide on phones). */
export function ConversationList({ conversations }: { conversations: ConversationSummary[] }) {
  const { t, locale } = useI18n();
  const pathname = usePathname();
  const openId = pathname.split("/")[2];

  return (
    <aside
      className={cn(
        "card flex min-h-0 flex-col overflow-hidden lg:w-96 lg:shrink-0",
        openId ? "max-lg:hidden" : "max-lg:flex-1", // on phones: list OR chat
      )}
    >
      <div className="border-b border-border px-5 py-4">
        <h2 className="text-base font-bold text-brand-700">{t.conversations.title}</h2>
        <p className="text-xs text-muted">{t.conversations.description}</p>
      </div>
      <ul className="min-h-0 flex-1 divide-y divide-border overflow-y-auto">
        {conversations.map((c) => {
          const name = c.customerName ?? `+${c.customerWaId}`;
          const active = c.id === openId;
          return (
            <li key={c.id}>
              <Link
                href={`/conversations/${c.id}`}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "flex items-center gap-3 px-4 py-3 transition",
                  active ? "bg-primary-soft" : "hover:bg-background",
                )}
              >
                <span className="grid size-11 shrink-0 place-items-center rounded-full bg-primary text-base font-bold text-primary-foreground">
                  {initials(name)}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="flex items-center justify-between gap-2">
                    <span dir="auto" className="truncate text-start font-bold text-brand-800">
                      {name}
                    </span>
                    <span className="shrink-0 text-[11px] text-muted" data-ltr>
                      {formatChatTime(c.lastMessageAt, locale)}
                    </span>
                  </span>
                  <span className="mt-0.5 flex items-center gap-1.5 text-sm text-muted">
                    {c.lastDirection === "OUTBOUND" && <Bot size={14} className="shrink-0 text-primary" aria-label={t.conversations.botReplied} />}
                    <span dir="auto" className="truncate text-start">
                      {c.lastMessage}
                    </span>
                    {c.simulated && (
                      <span className="ms-auto inline-flex shrink-0 items-center gap-1 rounded-full bg-gold-50 px-2 py-0.5 text-[10px] font-bold text-gold-700">
                        <FlaskConical size={11} aria-hidden />
                        {t.conversations.simulated}
                      </span>
                    )}
                  </span>
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </aside>
  );
}
