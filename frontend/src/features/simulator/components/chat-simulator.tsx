"use client";

import { Bot, RotateCcw, SendHorizontal, User } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { ChatBubble, ChatNotice } from "@/shared/components/chat-bubble";
import { useI18n } from "@/shared/i18n/provider";
import { ApiError } from "@/shared/lib/api-client";
import { errorMessage } from "@/shared/lib/errors";
import { formatTime } from "@/shared/lib/format";
import { Button, Card, Input } from "@/shared/ui";
import { pendingSimulatorReplies, sendSimulatorMessage } from "../api/simulator-api";

type Author = "customer" | "bot" | "system";

interface ChatMessage {
  id: number;
  author: Author;
  text: string;
  at: Date;
  /** customer messages only: has the shop's number (bot or a person) answered yet? */
  answered?: boolean;
}

/** A fresh fake customer per chat, so each chat has its own history on the server. */
function newCustomerId() {
  return `sim-${crypto.randomUUID().slice(0, 8)}`;
}

/** How often the simulator checks for replies the shop wrote in the dashboard. */
const PENDING_POLL_MS = 3000;

export function ChatSimulator() {
  const { t, locale } = useI18n();
  const [customerId, setCustomerId] = useState(newCustomerId);
  const [customerName, setCustomerName] = useState(t.simulator.defaultCustomerName);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const nextId = useRef(0);
  const endRef = useRef<HTMLDivElement>(null);
  /** The chat on screen, so a check that finishes after "New chat" doesn't land in the new chat. */
  const currentChat = useRef(customerId);
  useEffect(() => {
    currentChat.current = customerId;
  }, [customerId]);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, sending]);

  function add(author: Author, text: string): number {
    const id = nextId.current++;
    setMessages((current) => [...current, { id, author, text, at: new Date(), answered: false }]);
    return id;
  }

  /** Shows what the shop's number sent and marks the customer's messages as read. */
  const receive = useCallback((replies: string[]) => {
    setMessages((current) => [
      ...current.map((m) => (m.author === "customer" ? { ...m, answered: true } : m)),
      ...replies.map((text) => ({ id: nextId.current++, author: "bot" as const, text, at: new Date() })),
    ]);
  }, []);

  // replies the shop writes in the dashboard reach this chat outside the send/reply cycle: check for them
  const hasMessages = messages.length > 0;
  useEffect(() => {
    if (!hasMessages || sending) return;
    const check = async () => {
      if (document.visibilityState === "hidden") return;
      try {
        const replies = await pendingSimulatorReplies(customerId);
        // delivered even if a send started meanwhile: the server already handed these over
        if (replies.length > 0 && currentChat.current === customerId) receive(replies);
      } catch {
        // a missed check is retried on the next tick
      }
    };
    const timer = window.setInterval(check, PENDING_POLL_MS);
    return () => window.clearInterval(timer);
  }, [customerId, hasMessages, sending, receive]);

  function failureText(error: unknown) {
    if (error instanceof ApiError && error.problem.status === 504) return t.simulator.errors.timeout;
    if (error instanceof ApiError && error.problem.status === 404) return t.simulator.errors.disabled;
    return errorMessage(error, t);
  }

  async function send(event: React.FormEvent) {
    event.preventDefault();
    const text = draft.trim();
    if (!text || sending) return;

    add("customer", text);
    setDraft("");
    setSending(true);
    try {
      const reply = await sendSimulatorMessage({ customerId, customerName, text });
      // null: the shop took this chat over, so the bot stays silent and the shop's reply comes by polling
      if (reply !== null) receive([reply]);
    } catch (error) {
      add("system", failureText(error));
    } finally {
      setSending(false);
    }
  }

  function restart() {
    setCustomerId(newCustomerId());
    setMessages([]);
  }

  return (
    <Card className="flex h-[72vh] min-h-[480px] flex-col overflow-hidden">
      {/* chat header, WhatsApp style but in the brand color */}
      <div className="flex flex-wrap items-center gap-3 bg-brand-700 px-4 py-3 text-white">
        <span className="grid size-10 place-items-center rounded-full bg-gold-500 text-brand-900">
          <Bot size={20} aria-hidden />
        </span>
        <div className="min-w-0 flex-1">
          <p className="font-bold leading-tight">{t.simulator.botName}</p>
          <p className="text-xs text-brand-100">{sending ? t.simulator.typing : t.header.botOnline}</p>
        </div>
        <label className="flex items-center gap-2 text-xs text-brand-100">
          <User size={14} aria-hidden />
          <span className="max-sm:sr-only">{t.simulator.customerName}</span>
          <input
            className="h-8 w-32 rounded-lg border border-white/20 bg-white/10 px-2 text-sm text-white placeholder:text-white/60 focus:border-gold-500 focus:outline-none"
            value={customerName}
            onChange={(e) => setCustomerName(e.target.value)}
            maxLength={100}
          />
        </label>
        <Button variant="ghost" size="sm" className="text-white hover:bg-white/10" onClick={restart}>
          <RotateCcw size={14} aria-hidden />
          {t.simulator.newChat}
        </Button>
      </div>

      <div className="chat-wallpaper flex flex-1 flex-col gap-2 overflow-y-auto px-4 py-5 sm:px-8" aria-live="polite">
        {messages.length === 0 && (
          <p className="m-auto max-w-sm rounded-xl bg-white/90 px-4 py-3 text-center text-sm text-muted shadow-sm">
            {t.simulator.emptyHint}
          </p>
        )}
        {messages.map((m) =>
          m.author === "system" ? (
            <ChatNotice key={m.id} tone="danger">
              {m.text}
            </ChatNotice>
          ) : (
            // the customer is "me" in this view (you play the customer)
            <ChatBubble
              key={m.id}
              side={m.author === "customer" ? "mine" : "theirs"}
              text={m.text}
              time={formatTime(m.at, locale)}
              ticks={m.author === "customer" ? (m.answered ? "read" : "sent") : undefined}
            />
          ),
        )}
        {sending && (
          <div className="flex gap-1 self-start rounded-2xl rounded-es-sm bg-white px-4 py-3 shadow-sm" aria-label={t.simulator.typing}>
            {[0, 150, 300].map((delay) => (
              <span
                key={delay}
                className="size-2 animate-bounce rounded-full bg-chat-dots"
                style={{ animationDelay: `${delay}ms` }}
              />
            ))}
          </div>
        )}
        <div ref={endRef} />
      </div>

      <form onSubmit={send} className="flex items-center gap-2 border-t border-border bg-background px-3 py-3">
        <Input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder={t.simulator.placeholder}
          maxLength={2000}
          aria-label={t.simulator.inputLabel}
          dir="auto"
          className="rounded-full"
        />
        <Button
          type="submit"
          size="icon"
          className="size-11 shrink-0 rounded-full"
          disabled={sending || !draft.trim()}
          aria-label={t.simulator.send}
        >
          <SendHorizontal size={18} className="rtl:-scale-x-100" aria-hidden />
        </Button>
      </form>
    </Card>
  );
}
