import type { Dictionary } from "@/shared/i18n/dictionaries/ar";
import { ApiError } from "./api-client";

/** One message for any failed backend call, so every screen reports errors the same way. */
export function errorMessage(error: unknown, t: Dictionary): string {
  if (error instanceof ApiError) {
    if (error.problem.status === 400) return t.errors.badRequest;
    if (error.problem.status === 404) return t.errors.notFound;
    return t.errors.server;
  }
  return t.errors.network;
}
