import { NextResponse, type NextRequest } from "next/server";
import { SESSION_COOKIE } from "@/shared/auth/constants";

/**
 * Where an expired/rejected session is sent: clears the cookie and goes to the login page.
 * A GET that another site triggers (an <img> or link) must not sign the owner out, so cross-site
 * requests are only redirected. The sign-out button uses the logout Server Action instead.
 */
export function GET(request: NextRequest) {
  if (request.headers.get("sec-fetch-site") === "cross-site") {
    return NextResponse.redirect(new URL("/", request.url));
  }
  const response = NextResponse.redirect(new URL("/login", request.url));
  response.cookies.delete(SESSION_COOKIE);
  return response;
}
