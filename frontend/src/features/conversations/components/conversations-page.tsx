import { MessagesSquare } from "lucide-react";
import { ListPage, type ListPageConfig } from "@/shared/components/list-page";
import type { Locale } from "@/shared/i18n/config";
import type { Dictionary } from "@/shared/i18n/dictionaries/ar";
import { getI18n } from "@/shared/i18n/server";
import { formatDateTime } from "@/shared/lib/format";
import type { ConversationSummary } from "../types";

function config(t: Dictionary, locale: Locale): ListPageConfig<ConversationSummary> {
  const c = t.conversations.columns;
  return {
    title: t.conversations.title,
    description: t.conversations.description,
    rowKey: (row) => row.id,
    columns: [
      { key: "customer", header: c.customer, cell: (r) => r.customerName ?? r.customerWaId },
      {
        key: "last",
        header: c.lastMessage,
        cell: (r) => <span className="line-clamp-1 text-muted">{r.lastMessage}</span>,
      },
      { key: "updatedAt", header: c.time, cell: (r) => formatDateTime(r.updatedAt, locale) },
    ],
    empty: { title: t.conversations.emptyTitle, description: t.conversations.emptyDescription, icon: MessagesSquare },
  };
}

export async function ConversationsPage() {
  const { t, locale } = await getI18n();
  // Wired to the backend once the conversations endpoint exists.
  const conversations: ConversationSummary[] = [];
  return <ListPage config={config(t, locale)} rows={conversations} />;
}
