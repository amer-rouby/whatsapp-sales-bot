"use client";

import { useEffect, useRef, useState } from "react";
import { cn } from "@/shared/lib/cn";
import { ApiError } from "@/shared/lib/api-client";
import { Button, Card, Input } from "@/shared/ui";
import { sendSimulatorMessage } from "../api/simulator-api";

type Author = "customer" | "bot" | "system";

interface ChatMessage {
  id: number;
  author: Author;
  text: string;
}

const BUBBLE: Record<Author, string> = {
  customer: "self-end bg-primary text-primary-foreground",
  bot: "self-start bg-secondary text-foreground",
  system: "self-center bg-danger-soft text-danger text-xs",
};

/** A fresh fake customer per chat, so each chat has its own history on the server. */
function newCustomerId() {
  return `sim-${crypto.randomUUID().slice(0, 8)}`;
}

function errorText(error: unknown) {
  if (error instanceof ApiError && error.problem.status === 504) {
    return "البوت اتأخر في الرد. جرّب تاني.";
  }
  if (error instanceof ApiError && error.problem.status === 404) {
    return "المحاكي مقفول. شغّل الباك إند بـ SALESBOT_SIMULATOR_ENABLED=true";
  }
  return "مش قادر أوصل للباك إند. اتأكد إنه شغال على بورت 8080.";
}

export function ChatSimulator() {
  const [customerId, setCustomerId] = useState(newCustomerId);
  const [customerName, setCustomerName] = useState("عميل تجريبي");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const nextId = useRef(0);
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, sending]);

  function add(author: Author, text: string) {
    const id = nextId.current++;
    setMessages((current) => [...current, { id, author, text }]);
  }

  async function send(event: React.FormEvent) {
    event.preventDefault();
    const text = draft.trim();
    if (!text || sending) return;

    add("customer", text);
    setDraft("");
    setSending(true);
    try {
      add("bot", await sendSimulatorMessage({ customerId, customerName, text }));
    } catch (error) {
      add("system", errorText(error));
    } finally {
      setSending(false);
    }
  }

  function restart() {
    setCustomerId(newCustomerId());
    setMessages([]);
  }

  return (
    <Card className="flex h-[70vh] flex-col">
      <div className="flex flex-wrap items-center gap-3 border-b border-border p-3">
        <label className="flex items-center gap-2 text-sm text-muted">
          اسم العميل
          <Input
            className="h-8 w-40"
            value={customerName}
            onChange={(e) => setCustomerName(e.target.value)}
            maxLength={100}
          />
        </label>
        <Button variant="ghost" size="sm" className="ms-auto" onClick={restart}>
          محادثة جديدة
        </Button>
      </div>

      <div className="flex flex-1 flex-col gap-2 overflow-y-auto p-4" aria-live="polite">
        {messages.length === 0 && (
          <p className="m-auto text-sm text-muted">اكتب رسالة كأنك عميل، زي: &quot;بكام الكوتشي الأبيض؟&quot;</p>
        )}
        {messages.map((message) => (
          <p
            key={message.id}
            className={cn("max-w-[80%] whitespace-pre-wrap rounded-card px-3 py-2 text-sm", BUBBLE[message.author])}
          >
            {message.text}
          </p>
        ))}
        {sending && <p className="self-start text-xs text-muted">البوت بيكتب…</p>}
        <div ref={endRef} />
      </div>

      <form onSubmit={send} className="flex gap-2 border-t border-border p-3">
        <Input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="اكتب رسالتك…"
          maxLength={2000}
          aria-label="رسالة العميل"
        />
        <Button type="submit" disabled={sending || !draft.trim()}>
          إرسال
        </Button>
      </form>
    </Card>
  );
}
