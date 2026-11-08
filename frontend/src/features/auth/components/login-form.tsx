"use client";

import { LogIn } from "lucide-react";
import { useActionState } from "react";
import { login, type LoginState } from "@/shared/auth/actions";
import { useI18n } from "@/shared/i18n/provider";
import { Button, Field, Input } from "@/shared/ui";

/** Email + password form. The Server Action talks to the backend and sets the httpOnly session cookie. */
export function LoginForm({ next }: { next: string }) {
  const { t } = useI18n();
  const [state, action, pending] = useActionState<LoginState, FormData>(login, {});

  return (
    <form action={action} className="flex flex-col gap-5" noValidate>
      <input type="hidden" name="next" value={next} />
      <Field label={t.auth.email} htmlFor="email" required>
        <Input
          id="email"
          name="email"
          type="email"
          autoComplete="username"
          dir="ltr"
          required
          autoFocus
          defaultValue={state.email}
          aria-invalid={state.error ? true : undefined}
        />
      </Field>
      <Field label={t.auth.password} htmlFor="password" required>
        <Input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          dir="ltr"
          required
          aria-invalid={state.error ? true : undefined}
        />
      </Field>

      {state.error && (
        <p role="alert" className="rounded-control bg-danger-soft px-4 py-3 text-sm font-bold text-danger">
          {t.auth.errors[state.error]}
        </p>
      )}

      <Button type="submit" size="lg" disabled={pending} className="w-full">
        <LogIn size={18} aria-hidden />
        {pending ? t.auth.submitting : t.auth.submit}
      </Button>
    </form>
  );
}
