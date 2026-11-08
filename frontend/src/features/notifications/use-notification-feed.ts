"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { notificationsApi } from "./api/notifications-api";
import type { NotificationItem } from "./types";

const POLL_MS = 5000;
const KEEP = 50;

function byNewest(a: NotificationItem, b: NotificationItem) {
  return Date.parse(b.at) - Date.parse(a.at);
}

/**
 * Polls the backend feed and keeps the newest items. `onNews` gets only items that arrived after the
 * first load (history is shown, never announced). The server's clock decides what is new: each poll
 * passes back the previous `serverTime`, and the overlap the server adds is removed here by id.
 */
export function useNotificationFeed(onNews: (fresh: NotificationItem[]) => void) {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [serverTime, setServerTime] = useState<string | null>(null);
  const after = useRef<string | null>(null);
  const known = useRef(new Set<string>());
  const announce = useRef(onNews);

  useEffect(() => {
    announce.current = onNews; // latest callback without restarting the timer
  }, [onNews]);

  const poll = useCallback(async () => {
    try {
      const page = await notificationsApi.feed(after.current);
      const firstLoad = after.current === null;
      after.current = page.serverTime;
      setServerTime(page.serverTime);

      const fresh = page.items.filter((item) => !known.current.has(item.id));
      if (fresh.length === 0) return;
      fresh.forEach((item) => known.current.add(item.id));
      setItems((previous) => [...fresh, ...previous].sort(byNewest).slice(0, KEEP));
      if (!firstLoad) announce.current(fresh.sort(byNewest));
    } catch {
      // offline or backend restarting: the next tick tries again
    }
  }, []);

  useEffect(() => {
    const first = window.setTimeout(poll, 0);
    // keeps polling in background tabs too: that is exactly when the sound matters
    const timer = window.setInterval(poll, POLL_MS);
    const onVisible = () => document.visibilityState === "visible" && void poll();
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      window.clearTimeout(first);
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [poll]);

  return { items, serverTime };
}
