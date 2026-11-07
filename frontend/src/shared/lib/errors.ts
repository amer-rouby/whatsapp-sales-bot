import { ApiError } from "./api-client";

/** One Arabic message for any failed backend call, so every screen reports errors the same way. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.problem.status === 400) return "في بيانات ناقصة أو غلط. راجع الخانات وجرّب تاني.";
    if (error.problem.status === 404) return "العنصر ده مش موجود، ممكن يكون اتمسح.";
    return "حصلت مشكلة في السيرفر. جرّب تاني بعد شوية.";
  }
  return "مش قادر أوصل للباك إند. اتأكد إنه شغال على بورت 8080.";
}
