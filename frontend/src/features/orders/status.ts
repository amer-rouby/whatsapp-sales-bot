import type { BadgeTone } from "@/shared/ui";
import type { OrderStatus } from "./types";

/** Single source for how each status is named and coloured across the orders feature. */
export const STATUS_META: Record<OrderStatus, { label: string; tone: BadgeTone }> = {
  NEW: { label: "جديد", tone: "warning" },
  CONFIRMED: { label: "مؤكد", tone: "success" },
  SHIPPED: { label: "اتشحن", tone: "neutral" },
  CANCELLED: { label: "ملغي", tone: "danger" },
};
