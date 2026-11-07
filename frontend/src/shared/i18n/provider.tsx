"use client";

import { createContext, useContext } from "react";
import type { Locale } from "./config";
import { ar, type Dictionary } from "./dictionaries/ar";
import { en } from "./dictionaries/en";

const DICTIONARIES: Record<Locale, Dictionary> = { ar, en };

const I18nContext = createContext<{ locale: Locale; t: Dictionary }>({ locale: "ar", t: ar });

/** Gives client components the same locale the server rendered with. */
export function I18nProvider({ locale, children }: { locale: Locale; children: React.ReactNode }) {
  return <I18nContext.Provider value={{ locale, t: DICTIONARIES[locale] }}>{children}</I18nContext.Provider>;
}

/** For client components: const { t, locale } = useI18n(); */
export function useI18n() {
  return useContext(I18nContext);
}
