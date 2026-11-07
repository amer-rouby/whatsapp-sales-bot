import "server-only";
import { getPreferences } from "@/shared/preferences/server";
import type { Locale } from "./config";
import { ar, type Dictionary } from "./dictionaries/ar";
import { en } from "./dictionaries/en";

const DICTIONARIES: Record<Locale, Dictionary> = { ar, en };

export function dictionaryFor(locale: Locale): Dictionary {
  return DICTIONARIES[locale];
}

/** For server components: the viewer's locale and its texts. */
export async function getI18n(): Promise<{ locale: Locale; t: Dictionary }> {
  const { locale } = await getPreferences();
  return { locale, t: dictionaryFor(locale) };
}
