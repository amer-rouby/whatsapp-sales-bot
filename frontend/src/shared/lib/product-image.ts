import { BACKEND_PREFIX } from "@/shared/auth/constants";

/**
 * A product photo through the proxy (it adds the session). Photo ids never change content, so the browser
 * caches them for good. A plain <img> is used, not next/image: its optimizer fetches without the session.
 */
export function productImageUrl(imageId: string, size: "thumb" | "full" = "thumb"): string {
  return `${BACKEND_PREFIX}/api/products/images/${imageId}?size=${size}`;
}
