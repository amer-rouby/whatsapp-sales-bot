"use client";

import { Globe, Palette } from "lucide-react";
import { useRouter } from "next/navigation";
import { useTransition } from "react";
import { useI18n } from "@/shared/i18n/provider";
import { cn } from "@/shared/lib/cn";
import { setLocale, setTheme } from "@/shared/preferences/actions";
import { THEMES, type Theme } from "@/shared/preferences/theme";

const HEADER_BUTTON =
  "inline-flex h-10 items-center gap-2 rounded-xl px-2 text-sm font-bold text-white transition hover:bg-white/10 sm:px-3 " +
  "focus-visible:outline-2 focus-visible:outline-gold-500 disabled:opacity-60";

const SWATCH: Record<Theme, string> = { navy: "#2a3470", maroon: "#7a1232" };

/** Flips Arabic ⇄ English; the server re-renders with the new direction in the same round trip. */
export function LanguageSwitcher() {
  const { t, locale } = useI18n();
  const router = useRouter();
  const [pending, start] = useTransition();

  return (
    <button
      type="button"
      className={HEADER_BUTTON}
      disabled={pending}
      aria-label={t.header.switchLanguageLabel}
      onClick={() =>
        start(async () => {
          await setLocale(locale === "ar" ? "en" : "ar");
          router.refresh();
        })
      }
    >
      <Globe size={18} aria-hidden />
      <span className="max-sm:sr-only">{t.header.switchLanguage}</span>
    </button>
  );
}

/** Two-option theme picker shown as color swatches. */
export function ThemeSwitcher({ current }: { current: Theme }) {
  const { t } = useI18n();
  const router = useRouter();
  const [pending, start] = useTransition();

  return (
    <div className="flex items-center gap-1 rounded-xl bg-white/10 p-1" role="radiogroup" aria-label={t.header.theme}>
      <Palette size={16} className="mx-1 text-white/70 max-sm:hidden" aria-hidden />
      {THEMES.map((theme) => {
        const selected = theme === current;
        return (
          <button
            key={theme}
            type="button"
            role="radio"
            aria-checked={selected}
            title={t.header.themes[theme]}
            aria-label={t.header.themes[theme]}
            disabled={pending}
            onClick={() =>
              start(async () => {
                // instant feedback, then persist (the server render confirms it)
                document.documentElement.dataset.theme = theme;
                await setTheme(theme);
                router.refresh();
              })
            }
            className={cn(
              "grid size-6 place-items-center rounded-lg transition sm:size-7",
              selected ? "ring-2 ring-gold-500 ring-offset-2 ring-offset-brand-700" : "hover:scale-110",
            )}
          >
            <span className="size-4 rounded-full border border-white/40" style={{ background: SWATCH[theme] }} />
          </button>
        );
      })}
    </div>
  );
}
