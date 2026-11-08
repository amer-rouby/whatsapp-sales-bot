"use server";

import { cookies, headers } from "next/headers";
import { redirect } from "next/navigation";
import { BACKEND_URL, isHttps, safeNext, SESSION_COOKIE } from "./constants";

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
  if (response.status === 400 || response.status === 401) return { error: "invalid", email };
  if (!response.ok) return { error: "unreachable", email };

  const session = await readSession(response);
  if (!session) return { error: "unreachable", email };

  (await cookies()).set(SESSION_COOKIE, session.token, {
    httpOnly: true,
    // Secure only when the browser came over HTTPS: on plain http (e.g. a phone on the LAN) a Secure
    // cookie would be dropped silently and every login would bounce back to /login.
    secure: isHttps(await headers()),
    sameSite: "lax",
    path: "/",
    expires: session.expiresAt,
  });
  redirect(next);
}

/** The login response, or null if a gateway answered 2xx with something that isn't one. */
async function readSession(response: Response): Promise<{ token: string; expiresAt: Date } | null> {
  try {
    const body = (await response.json()) as { token?: unknown; expiresAt?: unknown };
    const expiresAt = new Date(String(body.expiresAt));
    if (typeof body.token !== "string" || !body.token || Number.isNaN(expiresAt.getTime())) return null;
    return { token: body.token, expiresAt };
  } catch {
    return null;
  }
}

export async function logout() {
  (await cookies()).delete(SESSION_COOKIE);
  redirect("/login");
}
