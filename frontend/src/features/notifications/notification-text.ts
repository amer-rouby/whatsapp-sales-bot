import type { Locale } from "@/shared/i18n/config";
import type { Dictionary } from "@/shared/i18n/dictionaries/ar";
import { formatMoney } from "@/shared/lib/format";
import type { NotificationItem } from "./types";

/** Where clicking a notification goes. */
export function notificationHref(item: NotificationItem) {
  if (item.type === "CUSTOMER_MESSAGE" && item.conversationId) return `/conversations/${item.conversationId}`;
  return "/orders";
}

/** Title, body and attention label: one wording for the bell, the toasts and the desktop notification. */
export function notificationText(item: NotificationItem, t: Dictionary, locale: Locale) {
  const name = item.customerName ?? `+${item.customerContact}`;
  if (item.type === "NEW_ORDER") {
    const total = item.total != null && item.currency ? formatMoney(item.total, item.currency, locale) : null;
    return {
      title: t.notifications.newOrder,
      name,
      body: total ? `${name} · ${total}` : name,
      attention: item.needsAttention ? t.notifications.needsReview : null,
    };
  }
  return {
    title: t.notifications.newMessage,
    name,
    body: item.preview ?? "",
    attention: item.needsAttention ? t.notifications.needsReply : null,
  };
}
