import { apiClient } from "@/shared/lib/api-client";
import type { Order, OrderStatus } from "../types";

const BASE = "/api/orders";

export const ordersApi = {
  list: () => apiClient.get<Order[]>(BASE, { cache: "no-store" }),
  changeStatus: (id: string, status: OrderStatus) => apiClient.patch<Order>(`${BASE}/${id}/status`, { status }),
};
