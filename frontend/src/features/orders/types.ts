import type { components } from "@/shared/api/schema";

type OrderResponse = components["schemas"]["OrderResponse"];

/** Generated from the backend's OpenAPI; these fields are always present in responses. */
export type Order = OrderResponse &
  Required<Pick<OrderResponse, "id" | "customerPhone" | "status" | "needsReview" | "items" | "createdAt">>;

export type OrderLine = components["schemas"]["OrderLineResponse"];
export type OrderStatus = Order["status"];

export const ORDER_STATUSES: OrderStatus[] = ["NEW", "CONFIRMED", "SHIPPED", "CANCELLED"];
