import "server-only";
import { cookies } from "next/headers";
import { DEFAULT_LOCALE, isLocale, LOCALE_COOKIE, type Locale } from "@/shared/i18n/config";
import { DEFAULT_THEME, isTheme, THEME_COOKIE, type Theme } from "./theme";

/** The viewer's language and theme, read on the server so the first paint is already right. */
export async function getPreferences(): Promise<{ locale: Locale; theme: Theme }> {
  const store = await cookies();
  const locale = store.get(LOCALE_COOKIE)?.value;
  const theme = store.get(THEME_COOKIE)?.value;
  return {
    locale: isLocale(locale) ? locale : DEFAULT_LOCALE,
    theme: isTheme(theme) ? theme : DEFAULT_THEME,
  };
}
