const LOCALE = "ar-EG";

export function formatMoney(amount: number, currency: string) {
  return new Intl.NumberFormat(LOCALE, { style: "currency", currency }).format(amount);
}

export function formatDateTime(iso: string) {
  return new Intl.DateTimeFormat(LOCALE, { dateStyle: "medium", timeStyle: "short" }).format(
    new Date(iso),
  );
}
