import "server-only";
import { cookies } from "next/headers";
import { isExpired, readTokenPayload, SESSION_COOKIE, type SessionUser } from "./constants";

/** The signed-in user for display (name in the header). Authorization itself is enforced by the backend. */
export async function getSessionUser(): Promise<SessionUser | null> {
  const token = (await cookies()).get(SESSION_COOKIE)?.value;
  const payload = token ? readTokenPayload(token) : null;
  if (!payload || isExpired(payload)) return null;
  return { email: payload.email, name: payload.name, role: payload.role };
}
