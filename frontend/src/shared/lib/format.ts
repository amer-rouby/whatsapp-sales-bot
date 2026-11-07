import { intlLocaleOf, type Locale } from "@/shared/i18n/config";

export function formatMoney(amount: number, currency: string, locale: Locale) {
  // whole prices without ".00" (1,250 EGP), cents only when there are any (99.50)
  const whole = Number.isInteger(Number(amount));
  return new Intl.NumberFormat(intlLocaleOf(locale), {
    style: "currency",
    currency,
    minimumFractionDigits: whole ? 0 : 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function formatNumber(value: number, locale: Locale) {
  return new Intl.NumberFormat(intlLocaleOf(locale)).format(value);
}

export function formatDateTime(iso: string, locale: Locale) {
  return new Intl.DateTimeFormat(intlLocaleOf(locale), { dateStyle: "medium", timeStyle: "short" }).format(
    new Date(iso),
  );
}

export function formatTime(date: Date, locale: Locale) {
  return new Intl.DateTimeFormat(intlLocaleOf(locale), { timeStyle: "short" }).format(date);
}
