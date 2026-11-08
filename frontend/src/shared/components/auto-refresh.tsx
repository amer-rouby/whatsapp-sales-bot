"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

/**
 * Re-renders the current server page every few seconds so new data (e.g. incoming WhatsApp messages)
 * shows up without a reload. Paused while the tab is hidden, so idle tabs don't poll the backend.
 */
export function AutoRefresh({ seconds = 8 }: { seconds?: number }) {
  const router = useRouter();

  useEffect(() => {
    const tick = () => {
      if (document.visibilityState === "visible") router.refresh();
    };
    const timer = window.setInterval(tick, seconds * 1000);
    document.addEventListener("visibilitychange", tick); // refresh right away when coming back
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", tick);
    };
  }, [router, seconds]);

  return null;
}
