import { Sparkles } from "lucide-react";
import { safeNext } from "@/shared/auth/constants";
import { getI18n } from "@/shared/i18n/server";
import { LanguageSwitcher } from "@/shared/layout/preference-switchers";
import { LoginForm } from "./login-form";

/** Split screen in the brand colors: story on the brand panel, the form on the other side. */
export async function LoginPage({ next }: { next?: string }) {
  const { t } = await getI18n();

  return (
    <div className="flex min-h-screen flex-col lg:flex-row">
      <section className="relative flex flex-col justify-between gap-8 overflow-hidden bg-brand-700 px-6 py-8 text-white lg:w-1/2 lg:px-14 lg:py-12">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="grid size-12 place-items-center rounded-2xl bg-gold-500 text-brand-900 shadow-lg">
              <Sparkles size={24} strokeWidth={2.2} aria-hidden />
            </span>
            <span>
              <span className="block text-2xl font-extrabold leading-tight">{t.app.name}</span>
              <span className="block text-xs text-brand-200">{t.app.tagline}</span>
            </span>
          </div>
          <LanguageSwitcher />
        </div>

        <div className="max-w-md max-lg:hidden">
          <h2 className="text-4xl font-extrabold leading-snug">{t.auth.brandHeadline}</h2>
          <p className="mt-4 text-lg leading-relaxed text-brand-100">{t.auth.brandText}</p>
        </div>

        <span className="h-1 w-24 rounded-full bg-gold-500 max-lg:hidden" aria-hidden />
      </section>

      <main className="flex flex-1 items-center justify-center bg-background px-4 py-10 sm:px-6">
        <div className="w-full max-w-sm animate-fade-up">
          <h1 className="text-3xl font-extrabold text-brand-800">{t.auth.title}</h1>
          <p className="mb-8 mt-2 text-muted">{t.auth.description}</p>
          <LoginForm next={safeNext(next)} />
        </div>
      </main>
    </div>
  );
}
