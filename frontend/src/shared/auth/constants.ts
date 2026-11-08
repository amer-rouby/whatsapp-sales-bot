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

/** First value of a header a reverse proxy may have turned into a comma-separated list. */
function firstValue(value: string | null): string | null {
  return value?.split(",")[0].trim() || null;
}

/**
 * The host the browser actually used. Behind cloudflared/nginx the Host header can be the internal
 * address, so the proxy's X-Forwarded-Host wins (the same rule Next.js uses for Server Actions).
 */
function publicHost(headers: Headers): string | null {
  return firstValue(headers.get("x-forwarded-host")) ?? firstValue(headers.get("host"));
}

/** True unless the browser says the request came from another site (no Origin header = not a cross-site browser request). */
export function isSameOrigin(headers: Headers): boolean {
  const origin = headers.get("origin");
  if (origin === null) return true;
  try {
    return new URL(origin).host === publicHost(headers);
  } catch {
    return false; // "null" or a malformed origin
  }
}

/** Whether the browser reached us over HTTPS (directly or through a TLS-terminating proxy/tunnel). */
export function isHttps(headers: Headers): boolean {
  const proto = firstValue(headers.get("x-forwarded-proto"));
  if (proto) return proto === "https";
  return headers.get("origin")?.startsWith("https:") ?? false;
}
