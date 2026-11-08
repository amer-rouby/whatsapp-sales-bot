"use client";

import { Bot, BotOff } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useI18n } from "@/shared/i18n/provider";
import { cn } from "@/shared/lib/cn";
import { errorMessage } from "@/shared/lib/errors";
import { conversationsApi } from "../api/conversations-api";

/** Hand the chat to the bot or keep it with the shop. Lives in the dark chat header. */
export function BotToggle({ conversationId, paused }: { conversationId: string; paused: boolean }) {
  const router = useRouter();
  const { t } = useI18n();
  const [busy, setBusy] = useState(false);

  async function toggle() {
    setBusy(true);
    try {
      await conversationsApi.setBotPaused(conversationId, !paused);
      router.refresh();
    } catch (error) {
      window.alert(errorMessage(error, t));
    } finally {
      setBusy(false);
    }
  }

  const label = paused ? t.conversations.resumeBot : t.conversations.pauseBot;
  const Icon = paused ? Bot : BotOff;
  return (
    <button
      type="button"
      onClick={toggle}
      disabled={busy}
      title={label}
      aria-label={label}
      className={cn(
        "inline-flex h-9 shrink-0 items-center gap-1.5 rounded-full px-3 text-xs font-bold transition disabled:opacity-60",
        "focus-visible:outline-2 focus-visible:outline-gold-500",
        paused ? "bg-gold-500 text-brand-900 hover:bg-gold-600" : "bg-white/10 text-white hover:bg-white/20",
      )}
    >
      <Icon size={15} aria-hidden />
      <span className="max-sm:sr-only">{label}</span>
    </button>
  );
}
