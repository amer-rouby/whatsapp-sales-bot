import { ListPage, type ListPageConfig } from "@/shared/components/list-page";
import { formatDateTime } from "@/shared/lib/format";
import type { ConversationSummary } from "../types";

const config: ListPageConfig<ConversationSummary> = {
  title: "المحادثات",
  description: "كل اللي العملاء كتبوه والبوت رد عليه",
  rowKey: (c) => c.id,
  columns: [
    { key: "customer", header: "العميل", cell: (c) => c.customerName ?? c.customerWaId },
    { key: "last", header: "آخر رسالة", cell: (c) => <span className="line-clamp-1 text-muted">{c.lastMessage}</span> },
    { key: "updatedAt", header: "الوقت", cell: (c) => formatDateTime(c.updatedAt) },
  ],
  empty: { title: "لسه مفيش محادثات", description: "أول ما عميل يبعت رسالة على الواتساب هتظهر هنا" },
};

export function ConversationsPage() {
  // Wired to the backend once the conversations endpoint exists.
  const conversations: ConversationSummary[] = [];
  return <ListPage config={config} rows={conversations} />;
}
