/** httpOnly cookie holding the backend's signed token. JavaScript in the page can never read it. */
export const SESSION_COOKIE = "session";

/** Browser calls go to this same-origin prefix; proxy.ts forwards them to the backend with the token. */
export const BACKEND_PREFIX = "/backend";

/** Where the Next.js server reaches the backend (server-side only, never shipped to the browser). */
export const BACKEND_URL = process.env.SALESBOT_BACKEND_URL ?? "http://localhost:8080";

export interface SessionUser {
  email: string;
  name: string;
  role: string;
}

/**
 * Reads the token's payload WITHOUT verifying it — only to show the user's name and to skip
 * obviously expired tokens. The backend verifies the signature on every API call.
 */
export function readTokenPayload(token: string): (SessionUser & { exp: number }) | null {
  try {
    const part = token.split(".")[1];
    const json = atob(part.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(part.length / 4) * 4, "="));
    const payload = JSON.parse(new TextDecoder().decode(Uint8Array.from(json, (c) => c.charCodeAt(0))));
    if (typeof payload.exp !== "number") return null;
    return { email: payload.email, name: payload.name, role: payload.role, exp: payload.exp };
  } catch {
    return null;
  }
}

export function isExpired(payload: { exp: number }, nowMs = Date.now()) {
  return payload.exp * 1000 <= nowMs;
}

/** Only same-site relative paths, so ?next= can't be used to send people to another site. */
export function safeNext(next: string | null | undefined): string {
  return next && next.startsWith("/") && !next.startsWith("//") && !next.startsWith("/\\") ? next : "/";
}
