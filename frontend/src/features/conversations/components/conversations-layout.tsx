import { CloudOff, MessagesSquare } from "lucide-react";
import { AutoRefresh } from "@/shared/components/auto-refresh";
import { getI18n } from "@/shared/i18n/server";
import { errorMessage } from "@/shared/lib/errors";
import { load } from "@/shared/lib/load";
import { Card, EmptyState } from "@/shared/ui";
import { conversationsQueries } from "../api/conversations-queries";
import { ConversationList } from "./conversation-list";

/** Inbox frame: chat list on the start side, the open chat (children) next to it. Refreshes itself. */
export async function ConversationsLayout({ children }: { children: React.ReactNode }) {
  const { t } = await getI18n();
  const result = await load(conversationsQueries.list);

  if (!result.ok) {
    return (
      <Card>
        <EmptyState icon={CloudOff} title={t.conversations.loadError} description={errorMessage(result.error, t)} />
      </Card>
    );
  }
  if (result.data.length === 0) {
    return (
      <Card>
        <AutoRefresh />
        <EmptyState icon={MessagesSquare} title={t.conversations.emptyTitle} description={t.conversations.emptyDescription} />
      </Card>
    );
  }

  return (
    <div className="flex h-[calc(100dvh-9rem)] min-h-[480px] gap-4 animate-fade-up">
      <AutoRefresh />
      <ConversationList conversations={result.data} />
      {children}
    </div>
  );
}
