"use client";

import { useCallback, useSyncExternalStore } from "react";

/** Same-tab writes don't fire the browser's "storage" event, so we announce them ourselves. */
const CHANGE_EVENT = "bayaa:local-setting";

function read(key: string): string | null {
  try {
    return window.localStorage.getItem(key);
  } catch {
    return null; // private mode / blocked storage: behave like "never set"
  }
}

function subscribe(onChange: () => void) {
  window.addEventListener("storage", onChange); // other tabs
  window.addEventListener(CHANGE_EVENT, onChange); // this tab
  return () => {
    window.removeEventListener("storage", onChange);
    window.removeEventListener(CHANGE_EVENT, onChange);
  };
}

/**
 * A per-browser setting kept in localStorage and shared live between components and tabs.
 * Renders `fallback` on the server and before hydration, so markup always matches.
 */
export function useLocalSetting(key: string, fallback: string | null = null) {
  const value = useSyncExternalStore(
    subscribe,
    () => read(key) ?? fallback,
    () => fallback,
  );

  const setValue = useCallback(
    (next: string | null) => {
      try {
        if (next === null) window.localStorage.removeItem(key);
        else window.localStorage.setItem(key, next);
      } catch {
        // storage unavailable: the setting just won't be remembered
      }
      window.dispatchEvent(new Event(CHANGE_EVENT));
    },
    [key],
  );

  return [value, setValue] as const;
}
