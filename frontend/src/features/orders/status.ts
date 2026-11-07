import type { BadgeTone } from "@/shared/ui";
import type { OrderStatus } from "./types";

/** Single source for how each status is coloured across the app (labels live in the dictionaries). */
export const STATUS_TONE: Record<OrderStatus, BadgeTone> = {
  NEW: "warning",
  CONFIRMED: "brand",
  SHIPPED: "success",
  CANCELLED: "danger",
};
