import type { Metadata } from "next";
import { Tajawal } from "next/font/google";
import { directionOf } from "@/shared/i18n/config";
import { I18nProvider } from "@/shared/i18n/provider";
import { dictionaryFor } from "@/shared/i18n/server";
import { getPreferences } from "@/shared/preferences/server";
import "./globals.css";

const tajawal = Tajawal({
  variable: "--font-tajawal",
  subsets: ["arabic", "latin"],
  weight: ["400", "500", "700", "800"],
});

export async function generateMetadata(): Promise<Metadata> {
  const { locale } = await getPreferences();
  const t = dictionaryFor(locale);
  return { title: { default: t.app.name, template: `%s · ${t.app.name}` }, description: t.app.tagline };
}

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  // Language and theme come from cookies, so the very first paint has the right direction and colors.
  const { locale, theme } = await getPreferences();
  return (
    <html lang={locale} dir={directionOf(locale)} data-theme={theme} className={`${tajawal.variable} h-full`}>
      <body className="min-h-full">
        <I18nProvider locale={locale}>{children}</I18nProvider>
      </body>
    </html>
  );
}
