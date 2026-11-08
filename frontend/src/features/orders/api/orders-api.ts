import { apiClient } from "@/shared/lib/api-client";
import type { Order, OrderStatus } from "../types";

/** Changes from client components (through proxy.ts). Reads live in orders-queries.ts. */
export const ordersApi = {
  changeStatus: (id: string, status: OrderStatus) => apiClient.patch<Order>(`/api/orders/${id}/status`, { status }),
};
