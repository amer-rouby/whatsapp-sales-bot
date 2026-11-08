import { ArrowRight, FlaskConical, Hand, MessageSquareText } from "lucide-react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ChatBubble, ChatNotice } from "@/shared/components/chat-bubble";
import { ScrollToEnd } from "@/shared/components/scroll-to-end";
import { fmt } from "@/shared/i18n/config";
import { getI18n } from "@/shared/i18n/server";
import { ApiError } from "@/shared/lib/api-client";
import { formatDate, formatTime } from "@/shared/lib/format";
import { productImageUrl } from "@/shared/lib/product-image";
import { EmptyState } from "@/shared/ui";
import { conversationsQueries } from "../api/conversations-queries";
import { BotToggle } from "./bot-toggle";
import { ReplyBox } from "./reply-box";

const CHAT_PANEL = "card flex min-h-0 flex-1 flex-col overflow-hidden";

/** Shown next to the list when no chat is open (desktop only; phones see the list). */
export async function NoConversationSelected() {
  const { t } = await getI18n();
  return (
    <section className={`${CHAT_PANEL} max-lg:hidden justify-center`}>
      <EmptyState icon={MessageSquareText} title={t.conversations.selectTitle} description={t.conversations.selectDescription} />
    </section>
  );
}

/** One chat from the shop's point of view: the bot's and the shop's replies are "mine" (green). The shop can answer here. */
export async function ConversationThread({ id }: { id: string }) {
  const { t, locale } = await getI18n();
  const chat = await conversationsQueries.get(id).catch((error: unknown) => {
    if (error instanceof ApiError && error.problem.status === 404) notFound();
    throw error;
  });
  const name = chat.customerName ?? `+${chat.customerWaId}`;
  const days = chat.messages.map((m) => formatDate(m.sentAt, locale));

  return (
    <section className={CHAT_PANEL}>
      <header className="flex items-center gap-3 bg-brand-700 px-4 py-3 text-white">
        <Link
          href="/conversations"
          className="grid size-9 place-items-center rounded-lg hover:bg-white/10 lg:hidden"
          aria-label={t.conversations.back}
        >
          <ArrowRight size={18} className="ltr:rotate-180" aria-hidden />
        </Link>
        <span className="grid size-10 place-items-center rounded-full bg-gold-500 font-bold text-brand-900">
          {name.trim().slice(0, 1).toUpperCase()}
        </span>
        <div className="min-w-0 flex-1">
          <p dir="auto" className="truncate text-start font-bold">
            {name}
          </p>
          <p className="text-xs text-brand-100" data-ltr>
            {chat.simulated ? t.conversations.simulated : `+${chat.customerWaId}`}
          </p>
        </div>
        {chat.simulated && <FlaskConical size={18} className="text-gold-300" aria-label={t.conversations.simulated} />}
        <span className="text-xs text-brand-100 max-md:hidden">{fmt(t.conversations.messageCount, { count: chat.messages.length })}</span>
        <BotToggle conversationId={chat.id} paused={chat.botPaused} />
      </header>

      {chat.botPaused && (
        <p role="status" className="flex items-center gap-2 border-b border-gold-100 bg-gold-50 px-4 py-2 text-xs font-bold text-gold-700">
          <Hand size={14} className="shrink-0" aria-hidden />
          {t.conversations.botPausedBanner}
        </p>
      )}

      <div className="chat-wallpaper flex min-h-0 flex-1 flex-col gap-2 overflow-y-auto px-4 py-5 sm:px-8">
        {chat.messages.map((m, i) => {
          // a date separator before the first message of each day
          const separator = i === 0 || days[i] !== days[i - 1] ? <ChatNotice key={`d-${m.id}`}>{days[i]}</ChatNotice> : null;
          const mine = m.direction === "OUTBOUND";
          const label = m.author === "AGENT" ? t.conversations.youLabel : mine ? t.conversations.botLabel : undefined;
          return [
            separator,
            <ChatBubble
              key={m.id}
              side={mine ? "mine" : "theirs"}
              label={label}
              text={m.imageId ? photoCaption(m.body) : m.body}
              imageUrl={m.imageId ? productImageUrl(m.imageId, "full") : undefined}
              time={formatTime(new Date(m.sentAt), locale)}
            />,
          ];
        })}
        <ScrollToEnd trigger={chat.messages.length} />
      </div>

      <ReplyBox key={chat.id} conversationId={chat.id} canReply={chat.canReply} />
    </section>
  );
}

/** "[صورة: «ساعة»]" (how a sent photo is kept for the AI) → "ساعة" as the photo's caption. */
function photoCaption(body: string) {
  return body.match(/«(.+)»/)?.[1] ?? body;
}
