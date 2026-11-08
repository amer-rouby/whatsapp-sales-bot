import { NextResponse, type NextRequest } from "next/server";
import { BACKEND_PREFIX, BACKEND_URL, isExpired, readTokenPayload, SESSION_COOKIE } from "@/shared/auth/constants";

/**
 * Runs before every request:
 * - /backend/*  → forwarded to the backend with the token from the httpOnly cookie (no CORS, no token in JS)
 * - /login      → signed-in users are sent to the dashboard
 * - everything else needs a (non-expired) session, otherwise → /login?next=…
 * The backend still verifies the token's signature on every call; this only routes people.
 */
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  const token = request.cookies.get(SESSION_COOKIE)?.value;
  const payload = token ? readTokenPayload(token) : null;
  const signedIn = payload !== null && !isExpired(payload);

  if (pathname.startsWith(`${BACKEND_PREFIX}/`)) {
    // Changes only from our own pages (on top of the SameSite cookie): another site can't use the session.
    const origin = request.headers.get("origin");
    if (!SAFE_METHODS.has(request.method) && origin !== null && origin !== request.nextUrl.origin) {
      return NextResponse.json({ status: 403, title: "Forbidden" }, { status: 403 });
    }
    const target = new URL(pathname.slice(BACKEND_PREFIX.length) + search, BACKEND_URL);
    const headers = new Headers(request.headers);
    headers.delete("cookie"); // the backend doesn't use cookies; don't leak them
    // Server-to-server from here: the browser's origin was checked above and would only trip the backend's CORS.
    headers.delete("origin");
    if (signedIn) headers.set("Authorization", `Bearer ${token}`);
    return NextResponse.rewrite(target, { request: { headers } });
  }

  if (pathname === "/login") {
    return signedIn ? NextResponse.redirect(new URL("/", request.url)) : NextResponse.next();
  }

  if (!signedIn) {
    const login = new URL("/login", request.url);
    if (pathname !== "/") login.searchParams.set("next", pathname + search);
    const response = NextResponse.redirect(login);
    if (token) response.cookies.delete(SESSION_COOKIE); // expired or unreadable
    return response;
  }

  return NextResponse.next();
}

export const config = {
  // everything except Next.js internals, static files and the logout handler (it must work without a session)
  matcher: ["/((?!_next/static|_next/image|favicon.ico|logout|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)"],
};
