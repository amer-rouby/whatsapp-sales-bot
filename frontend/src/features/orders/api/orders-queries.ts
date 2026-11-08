import "server-only";
import { serverApi } from "@/shared/lib/api-client.server";
import type { Order } from "../types";

/** Reads for server components: straight to the backend with the signed-in user's token. */
export const ordersQueries = {
  list: () => serverApi.get<Order[]>("/api/orders", { cache: "no-store" }),
};
