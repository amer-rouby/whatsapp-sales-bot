"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { BACKEND_URL, safeNext, SESSION_COOKIE } from "./constants";

export type LoginState = { error?: "invalid" | "tooMany" | "unreachable" | "missing"; email?: string };

/**
 * Exchanges email + password with the backend and stores the token in an httpOnly cookie.
 * Runs on the server: the password and the token never pass through browser JavaScript.
 */
export async function login(_previous: LoginState, form: FormData): Promise<LoginState> {
  const email = String(form.get("email") ?? "").trim();
  const password = String(form.get("password") ?? "");
  const next = safeNext(String(form.get("next") ?? ""));
  if (!email || !password) return { error: "missing", email };

  let response: Response;
  try {
    response = await fetch(`${BACKEND_URL}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ email, password }),
      cache: "no-store",
    });
  } catch {
    return { error: "unreachable", email };
  }
  if (response.status === 429) return { error: "tooMany", email };
  if (!response.ok) return { error: "invalid", email };

  const { token, expiresAt } = (await response.json()) as { token: string; expiresAt: string };
  (await cookies()).set(SESSION_COOKIE, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    expires: new Date(expiresAt),
  });
  redirect(next);
}

export async function logout() {
  (await cookies()).delete(SESSION_COOKIE);
  redirect("/login");
}
