import { Check, CheckCheck } from "lucide-react";
import { cn } from "@/shared/lib/cn";

export interface ChatBubbleProps {
  /** "mine" = right/end side in WhatsApp green; "theirs" = start side in white */
  side: "mine" | "theirs";
  text: string;
  /** already formatted for the viewer's locale */
  time: string;
  /** delivery ticks, only meaningful on "mine" */
  ticks?: "sent" | "read";
  /** small label above the text, e.g. the bot's name */
  label?: string;
}

/** One WhatsApp-style message. Used by the simulator and the conversations inbox, so both look identical. */
export function ChatBubble({ side, text, time, ticks, label }: ChatBubbleProps) {
  return (
    <div
      className={cn(
        "max-w-[78%] animate-fade-up rounded-2xl px-3 pb-1.5 pt-2 text-sm shadow-sm text-chat-ink",
        side === "mine" ? "self-end rounded-ee-sm bg-chat-out" : "self-start rounded-es-sm bg-white",
      )}
    >
      {label && <p className="mb-0.5 text-xs font-bold text-primary">{label}</p>}
      <p dir="auto" className="whitespace-pre-wrap text-start leading-relaxed">
        {text}
      </p>
      <p className="mt-0.5 flex items-center justify-end gap-1 text-[10px] text-chat-meta">
        <span data-ltr>{time}</span>
        {ticks === "read" && <CheckCheck size={14} className="text-chat-read" aria-hidden />}
        {ticks === "sent" && <Check size={14} aria-hidden />}
      </p>
    </div>
  );
}

/** Centered system line inside a chat (errors, date separators). */
export function ChatNotice({ children, tone = "neutral" }: { children: React.ReactNode; tone?: "neutral" | "danger" }) {
  return (
    <p
      className={cn(
        "self-center rounded-lg px-3 py-1.5 text-xs font-bold shadow-sm",
        tone === "danger" ? "bg-danger-soft text-danger" : "bg-white/90 text-chat-meta",
      )}
    >
      {children}
    </p>
  );
}
