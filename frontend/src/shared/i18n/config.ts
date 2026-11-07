export const LOCALES = ["ar", "en"] as const;
export type Locale = (typeof LOCALES)[number];
export const DEFAULT_LOCALE: Locale = "ar";
export const LOCALE_COOKIE = "locale";

export function isLocale(value: unknown): value is Locale {
  return typeof value === "string" && (LOCALES as readonly string[]).includes(value);
}

export function directionOf(locale: Locale): "rtl" | "ltr" {
  return locale === "ar" ? "rtl" : "ltr";
}

/**
 * Locale used by Intl for numbers, money and dates.
 * Arabic keeps Arabic month/currency names but Western digits (1250, not ١٢٥٠): clearer in a sales
 * dashboard and the same digits customers type on WhatsApp.
 */
export function intlLocaleOf(locale: Locale): string {
  return locale === "ar" ? "ar-EG-u-nu-latn" : "en-US";
}

/** Fills {placeholders}: fmt("Edit: {name}", { name: "Shoe" }) */
export function fmt(template: string, values: Record<string, string | number>): string {
  return template.replace(/\{(\w+)\}/g, (match, key: string) =>
    key in values ? String(values[key]) : match,
  );
}
