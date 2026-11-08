"use client";

import { Clock, SendHorizontal } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useI18n } from "@/shared/i18n/provider";
import { ApiError } from "@/shared/lib/api-client";
import { errorMessage } from "@/shared/lib/errors";
import { Button } from "@/shared/ui";
import { conversationsApi } from "../api/conversations-api";

const MAX_LENGTH = 4096; // WhatsApp's limit for one text message

/** The shop's own reply. Sending pauses the bot in this chat (the backend does it). */
export function ReplyBox({ conversationId, canReply }: { conversationId: string; canReply: boolean }) {
  const router = useRouter();
  const { t } = useI18n();
  const [text, setText] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!canReply) {
    return (
      <footer className="flex items-start gap-2 border-t border-border bg-warning-soft px-4 py-3 text-xs font-bold text-warning">
        <Clock size={16} className="mt-0.5 shrink-0" aria-hidden />
        <p>{t.conversations.windowClosed}</p>
      </footer>
    );
  }

  async function send() {
    const body = text.trim();
    if (!body || sending) return;
    setSending(true);
    setError(null);
    try {
      await conversationsApi.reply(conversationId, body);
      setText("");
      router.refresh();
    } catch (e) {
      if (e instanceof ApiError && e.problem.status === 409) setError(t.conversations.windowClosed);
      else if (e instanceof ApiError && e.problem.status === 502) setError(t.conversations.sendFailed);
      else setError(errorMessage(e, t));
    } finally {
      setSending(false);
    }
  }

  return (
    <footer className="border-t border-border bg-background px-3 py-3 sm:px-4">
      {error && (
        <p role="alert" className="mb-2 rounded-lg bg-danger-soft px-3 py-2 text-xs font-bold text-danger">
          {error}
        </p>
      )}
      <form
        className="flex items-end gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          void send();
        }}
      >
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => {
            // Enter sends, Shift+Enter is a new line; never while an IME is composing
            if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
              e.preventDefault();
              void send();
            }
          }}
          rows={1}
          maxLength={MAX_LENGTH}
          dir="auto"
          placeholder={t.conversations.replyPlaceholder}
          aria-label={t.conversations.replyLabel}
          disabled={sending}
          className="field-input max-h-40 min-h-11 flex-1 resize-none py-2.5 text-start [field-sizing:content]"
        />
        <Button type="submit" size="icon" className="size-11 shrink-0 rounded-full" disabled={sending || !text.trim()} aria-label={t.conversations.send}>
          <SendHorizontal size={18} className="rtl:-scale-x-100" aria-hidden />
        </Button>
      </form>
      <p className="mt-1.5 text-[11px] text-muted max-sm:hidden">{t.conversations.replyHint}</p>
    </footer>
  );
}
