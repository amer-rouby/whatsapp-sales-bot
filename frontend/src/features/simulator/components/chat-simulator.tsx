"use client";

import { Bot, Check, CheckCheck, RotateCcw, SendHorizontal, User } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useI18n } from "@/shared/i18n/provider";
import { ApiError } from "@/shared/lib/api-client";
import { cn } from "@/shared/lib/cn";
import { errorMessage } from "@/shared/lib/errors";
import { formatTime } from "@/shared/lib/format";
import { Button, Card, Input } from "@/shared/ui";
import { sendSimulatorMessage } from "../api/simulator-api";

type Author = "customer" | "bot" | "system";

interface ChatMessage {
  id: number;
  author: Author;
  text: string;
  at: Date;
  /** customer messages only: has the bot answered yet? */
  answered?: boolean;
}

/** A fresh fake customer per chat, so each chat has its own history on the server. */
function newCustomerId() {
  return `sim-${crypto.randomUUID().slice(0, 8)}`;
}

export function ChatSimulator() {
  const { t, locale } = useI18n();
  const [customerId, setCustomerId] = useState(newCustomerId);
  const [customerName, setCustomerName] = useState(t.simulator.defaultCustomerName);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const nextId = useRef(0);
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, sending]);

  function add(author: Author, text: string): number {
    const id = nextId.current++;
    setMessages((current) => [...current, { id, author, text, at: new Date(), answered: false }]);
    return id;
  }

  function markAnswered(id: number) {
    setMessages((current) => current.map((m) => (m.id === id ? { ...m, answered: true } : m)));
  }

  function failureText(error: unknown) {
    if (error instanceof ApiError && error.problem.status === 504) return t.simulator.errors.timeout;
    if (error instanceof ApiError && error.problem.status === 404) return t.simulator.errors.disabled;
    return errorMessage(error, t);
  }

  async function send(event: React.FormEvent) {
    event.preventDefault();
    const text = draft.trim();
    if (!text || sending) return;

    const id = add("customer", text);
    setDraft("");
    setSending(true);
    try {
      const reply = await sendSimulatorMessage({ customerId, customerName, text });
      markAnswered(id);
      add("bot", reply);
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
            <p key={m.id} className="self-center rounded-lg bg-danger-soft px-3 py-1.5 text-xs font-bold text-danger">
              {m.text}
            </p>
          ) : (
            <div
              key={m.id}
              className={cn(
                "max-w-[78%] animate-fade-up rounded-2xl px-3 pb-1.5 pt-2 text-sm shadow-sm",
                // the customer is "me" in this view → end side, WhatsApp green; the bot → start side, white
                m.author === "customer"
                  ? "self-end rounded-ee-sm bg-chat-out text-chat-ink"
                  : "self-start rounded-es-sm bg-white text-chat-ink",
              )}
            >
              <p dir="auto" className="whitespace-pre-wrap text-start leading-relaxed">{m.text}</p>
              <p className="mt-0.5 flex items-center justify-end gap-1 text-[10px] text-chat-meta">
                <span data-ltr>{formatTime(m.at, locale)}</span>
                {m.author === "customer" &&
                  (m.answered ? (
                    <CheckCheck size={14} className="text-chat-read" aria-hidden />
                  ) : (
                    <Check size={14} aria-hidden />
                  ))}
              </p>
            </div>
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
