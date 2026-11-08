import "server-only";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { BACKEND_URL, SESSION_COOKIE } from "@/shared/auth/constants";
import { createApiClient } from "./api-core";

/** For server components (reads). Talks to the backend directly with the signed-in user's token. */
export const serverApi = createApiClient({
  baseUrl: BACKEND_URL,
  headers: async (): Promise<Record<string, string>> => {
    const token = (await cookies()).get(SESSION_COOKIE)?.value;
    return token ? { Authorization: `Bearer ${token}` } : {};
  },
  onUnauthorized: () => redirect("/logout"),
});
