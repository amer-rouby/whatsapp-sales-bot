import { NextResponse, type NextRequest } from "next/server";
import { SESSION_COOKIE } from "@/shared/auth/constants";

/** Where an expired/rejected session is sent: clears the cookie and goes to the login page. */
export function GET(request: NextRequest) {
  const response = NextResponse.redirect(new URL("/login", request.url));
  response.cookies.delete(SESSION_COOKIE);
  return response;
}
