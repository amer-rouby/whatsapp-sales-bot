import { BACKEND_PREFIX } from "@/shared/auth/constants";
import { createApiClient } from "./api-core";

export { ApiError, type ProblemDetail } from "./api-core";

/**
 * For client components (mutations). Calls the same origin; proxy.ts adds the token from the
 * httpOnly cookie on the way to the backend, so the token never touches JavaScript.
 */
export const apiClient = createApiClient({
  baseUrl: BACKEND_PREFIX,
  onUnauthorized: () => {
    // session expired or revoked: clear it and go to the login page
    window.location.assign("/logout");
  },
});
