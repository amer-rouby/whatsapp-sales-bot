"use client";

import { Bell, BellRing, MonitorSmartphone, Volume2, VolumeX, X } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { fmt } from "@/shared/i18n/config";
import { useI18n } from "@/shared/i18n/provider";
import { cn } from "@/shared/lib/cn";
import { useLocalSetting } from "@/shared/lib/local-setting";
import { playChime, unlockSounds, type Chime } from "@/shared/lib/sounds";
import { notificationHref, notificationText } from "../notification-text";
import type { NotificationItem } from "../types";
import { useNotificationFeed } from "../use-notification-feed";
import { NotificationRow } from "./notification-row";

const SEEN_KEY = "bayaa.notifications.seenAt";
const SOUND_KEY = "bayaa.notifications.sound";
const DESKTOP_KEY = "bayaa.notifications.desktop";
const TOAST_MS = 7000;
const MAX_TOASTS = 3;

function chimeFor(fresh: NotificationItem[]): Chime {
  if (fresh.some((i) => i.needsAttention)) return "attention";
  if (fresh.some((i) => i.type === "NEW_ORDER")) return "order";
  return "message";
}

function desktopSupported() {
  return typeof window !== "undefined" && "Notification" in window && window.isSecureContext;
}

/**
 * The header bell: new customer messages and orders with a sound, a pop-up toast, an unread badge
 * (also in the tab title) and, when allowed, a system notification while the tab is in the background.
 */
export function NotificationBell() {
  const { t, locale } = useI18n();
  const router = useRouter();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const [toasts, setToasts] = useState<NotificationItem[]>([]);
  const [seenAt, setSeenAt] = useLocalSetting(SEEN_KEY);
  const [sound, setSound] = useLocalSetting(SOUND_KEY, "on");
  const [desktop, setDesktop] = useLocalSetting(DESKTOP_KEY, "off");
  const [permission, setPermission] = useState<NotificationPermission | "unsupported">("default");
  const panel = useRef<HTMLDivElement>(null);

  const soundOn = sound === "on";
  const desktopOn = desktop === "on" && permission === "granted";

  const onNews = useCallback(
    (fresh: NotificationItem[]) => {
      // the simulator user is the "customer": don't ring for their own test messages
      const news = pathname.startsWith("/simulator") ? fresh.filter((i) => !i.simulated) : fresh;
      router.refresh(); // counters, inbox and orders update right away
      if (news.length === 0) return;

      if (soundOn) playChime(chimeFor(news));
      setToasts((current) => [...news, ...current].slice(0, MAX_TOASTS));
      for (const item of news) {
        window.setTimeout(() => setToasts((current) => current.filter((x) => x.id !== item.id)), TOAST_MS);
      }
      if (desktopOn && document.visibilityState === "hidden") {
        for (const item of news.slice(0, MAX_TOASTS)) {
          const text = notificationText(item, t, locale);
          const note = new Notification(`${text.title} · ${text.name}`, { body: text.body, tag: item.id, lang: locale, dir: "auto" });
          note.onclick = () => {
            window.focus();
            router.push(notificationHref(item));
            note.close();
          };
        }
      }
    },
    [pathname, router, soundOn, desktopOn, t, locale],
  );

  const { items, serverTime } = useNotificationFeed(onNews);

  // first visit: start "read" from now, so old history isn't shown as unread
  useEffect(() => {
    if (serverTime && seenAt === null) setSeenAt(serverTime);
  }, [serverTime, seenAt, setSeenAt]);

  const seenMs = seenAt ? Date.parse(seenAt) : Number.POSITIVE_INFINITY;
  const unread = items.filter((i) => Date.parse(i.at) > seenMs).length;

  // "(3) Bayaa": the count shows even when the tab is in the background
  // Next.js rewrites the title on navigation and router.refresh(), so watch it and put the count back.
  useEffect(() => {
    const prefix = unread > 0 ? `(${unread > 9 ? "9+" : unread}) ` : "";
    const apply = () => {
      const wanted = prefix + document.title.replace(/^\(\d+\+?\) /, "");
      if (document.title !== wanted) document.title = wanted; // no change, no new mutation: no loop
    };
    apply();
    const observer = new MutationObserver(apply);
    observer.observe(document.head, { childList: true, subtree: true, characterData: true });
    return () => {
      observer.disconnect();
      document.title = document.title.replace(/^\(\d+\+?\) /, "");
    };
  }, [unread]);

  // browsers only allow sound after a user gesture: unlock on the first click/keypress anywhere
  useEffect(() => {
    const unlock = () => void unlockSounds();
    window.addEventListener("pointerdown", unlock, { once: true });
    window.addEventListener("keydown", unlock, { once: true });
    return () => {
      window.removeEventListener("pointerdown", unlock);
      window.removeEventListener("keydown", unlock);
    };
  }, []);

  // close on outside click / Escape
  useEffect(() => {
    if (!open) return;
    const onPointer = (e: PointerEvent) => {
      if (panel.current && !panel.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("pointerdown", onPointer);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("pointerdown", onPointer);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  function markAllRead() {
    const newest = items[0]?.at;
    const mark = [newest, serverTime].filter(Boolean).sort((a, b) => Date.parse(b!) - Date.parse(a!))[0];
    if (mark) setSeenAt(mark);
  }

  function toggleOpen() {
    setPermission(desktopSupported() ? Notification.permission : "unsupported");
    setOpen((v) => !v);
  }

  async function toggleSound() {
    if (soundOn) {
      setSound("off");
      return;
    }
    setSound("on");
    await unlockSounds();
    playChime("message");
  }

  async function toggleDesktop() {
    if (!desktopSupported()) return;
    if (desktopOn) {
      setDesktop("off");
      return;
    }
    const result = Notification.permission === "default" ? await Notification.requestPermission() : Notification.permission;
    setPermission(result);
    if (result === "granted") setDesktop("on");
  }

  const desktopLabel =
    permission === "unsupported"
      ? t.notifications.desktopUnsupported
      : permission === "denied"
        ? t.notifications.desktopBlocked
        : desktopOn
          ? t.notifications.on
          : t.notifications.off;

  const BellIcon = unread > 0 ? BellRing : Bell;

  return (
    <div ref={panel} className="relative">
      <button
        type="button"
        onClick={toggleOpen}
        aria-expanded={open}
        aria-haspopup="dialog"
        aria-label={unread > 0 ? `${t.notifications.title} — ${fmt(t.notifications.unreadCount, { count: unread })}` : t.notifications.title}
        title={t.notifications.title}
        className="relative grid size-10 place-items-center rounded-xl text-white transition hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-gold-500"
      >
        <BellIcon size={20} className={cn(unread > 0 && "animate-bell-ring")} aria-hidden />
        {unread > 0 && (
          <span className="absolute -top-0.5 end-0 grid h-5 min-w-5 place-items-center rounded-full bg-gold-500 px-1 text-[11px] font-extrabold text-brand-900 ring-2 ring-brand-700" data-ltr>
            {unread > 9 ? "9+" : unread}
          </span>
        )}
      </button>

      {open && (
        <div
          role="dialog"
          aria-label={t.notifications.title}
          className="card fixed inset-x-2 top-16 z-50 flex max-h-[min(36rem,calc(100dvh-5rem))] flex-col overflow-hidden text-foreground shadow-2xl sm:absolute sm:inset-x-auto sm:end-0 sm:top-full sm:mt-2 sm:w-[24rem] animate-fade-up"
        >
          <div className="flex items-center justify-between gap-2 border-b border-border px-4 py-3">
            <div>
              <p className="font-bold text-brand-700">{t.notifications.title}</p>
              {unread > 0 && <p className="text-xs text-muted">{fmt(t.notifications.unreadCount, { count: unread })}</p>}
            </div>
            {unread > 0 && (
              <button type="button" onClick={markAllRead} className="rounded-lg px-2 py-1 text-xs font-bold text-primary hover:bg-primary-soft">
                {t.notifications.markAllRead}
              </button>
            )}
          </div>

          <div className="min-h-0 flex-1 divide-y divide-border overflow-y-auto">
            {items.length === 0 ? (
              <div className="px-6 py-10 text-center">
                <Bell size={28} className="mx-auto mb-3 text-muted" aria-hidden />
                <p className="font-bold text-brand-800">{t.notifications.empty}</p>
                <p className="mt-1 text-xs text-muted">{t.notifications.emptyHint}</p>
              </div>
            ) : (
              items.map((item) => (
                <NotificationRow
                  key={item.id}
                  item={item}
                  unread={Date.parse(item.at) > seenMs}
                  onNavigate={() => {
                    setOpen(false);
                    markAllRead();
                  }}
                />
              ))
            )}
          </div>

          <div className="grid grid-cols-2 gap-2 border-t border-border bg-background p-3 text-xs">
            <button
              type="button"
              onClick={toggleSound}
              aria-pressed={soundOn}
              className={cn(
                "flex items-center gap-2 rounded-lg border px-3 py-2 font-bold transition",
                soundOn ? "border-primary/30 bg-primary-soft text-primary" : "border-border bg-surface text-muted hover:bg-secondary",
              )}
            >
              {soundOn ? <Volume2 size={16} aria-hidden /> : <VolumeX size={16} aria-hidden />}
              <span className="text-start">
                {t.notifications.sound}
                <span className="block text-[10px] font-normal">{soundOn ? t.notifications.on : t.notifications.off}</span>
              </span>
            </button>
            <button
              type="button"
              onClick={toggleDesktop}
              aria-pressed={desktopOn}
              disabled={permission === "unsupported" || permission === "denied"}
              className={cn(
                "flex items-center gap-2 rounded-lg border px-3 py-2 font-bold transition disabled:cursor-not-allowed disabled:opacity-70",
                desktopOn ? "border-primary/30 bg-primary-soft text-primary" : "border-border bg-surface text-muted hover:bg-secondary",
              )}
            >
              <MonitorSmartphone size={16} aria-hidden />
              <span className="text-start">
                {t.notifications.desktop}
                <span className="block text-[10px] font-normal">{desktopLabel}</span>
              </span>
            </button>
          </div>
        </div>
      )}

      {toasts.length > 0 && (
        // top, under the header: at the bottom they would cover the chat's reply box
        <div aria-live="polite" className="fixed end-4 top-20 z-[25] flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-2">
          {toasts.map((item) => (
            <div key={item.id} className="card relative overflow-hidden text-foreground shadow-2xl animate-fade-up">
              <span className={cn("absolute inset-y-0 start-0 w-1", item.needsAttention ? "bg-gold-500" : "bg-primary")} aria-hidden />
              <NotificationRow item={item} className="pe-10" onNavigate={() => setToasts((c) => c.filter((x) => x.id !== item.id))} />
              <button
                type="button"
                onClick={() => setToasts((c) => c.filter((x) => x.id !== item.id))}
                aria-label={t.notifications.dismiss}
                className="absolute end-2 top-2 grid size-7 place-items-center rounded-full text-muted hover:bg-secondary"
              >
                <X size={14} aria-hidden />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
