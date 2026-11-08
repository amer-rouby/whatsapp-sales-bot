"use client";

import { FlaskConical, MessageCircle, ShoppingBag } from "lucide-react";
import Link from "next/link";
import { useI18n } from "@/shared/i18n/provider";
import { cn } from "@/shared/lib/cn";
import { formatChatTime } from "@/shared/lib/format";
import { notificationHref, notificationText } from "../notification-text";
import type { NotificationItem } from "../types";

/** One notification line, used in the bell's list and in the pop-up toasts. */
export function NotificationRow({
  item,
  unread = false,
  onNavigate,
  className,
}: {
  item: NotificationItem;
  unread?: boolean;
  onNavigate?: () => void;
  className?: string;
}) {
  const { t, locale } = useI18n();
  const text = notificationText(item, t, locale);
  const isOrder = item.type === "NEW_ORDER";
  const Icon = isOrder ? ShoppingBag : MessageCircle;

  return (
    <Link
      href={notificationHref(item)}
      onClick={onNavigate}
      className={cn("flex items-start gap-3 px-4 py-3 text-start transition hover:bg-background", unread && "bg-primary-soft/60", className)}
    >
      <span
        className={cn(
          "grid size-10 shrink-0 place-items-center rounded-full",
          item.needsAttention ? "bg-gold-500 text-brand-900" : isOrder ? "bg-success-soft text-success" : "bg-primary-soft text-primary",
        )}
      >
        <Icon size={18} aria-hidden />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-center justify-between gap-2">
          <span className="truncate text-sm font-bold text-brand-800">
            {text.title}
            {!isOrder && (
              <>
                {" · "}
                <span dir="auto">{text.name}</span>
              </>
            )}
          </span>
          <span className="shrink-0 text-[11px] text-muted" data-ltr>
            {formatChatTime(item.at, locale)}
          </span>
        </span>
        <span dir="auto" className="mt-0.5 line-clamp-2 block text-start text-sm text-muted">
          {text.body}
        </span>
        {(text.attention || item.simulated) && (
          <span className="mt-1 flex flex-wrap gap-1.5">
            {text.attention && (
              <span className="rounded-full bg-gold-50 px-2 py-0.5 text-[10px] font-bold text-gold-700">{text.attention}</span>
            )}
            {item.simulated && (
              <span className="inline-flex items-center gap-1 rounded-full bg-secondary px-2 py-0.5 text-[10px] font-bold text-muted">
                <FlaskConical size={10} aria-hidden />
                {t.notifications.simulated}
              </span>
            )}
          </span>
        )}
      </span>
      {unread && <span className="mt-2 size-2 shrink-0 rounded-full bg-primary" aria-hidden />}
    </Link>
  );
}
