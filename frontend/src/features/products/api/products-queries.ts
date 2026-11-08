import "server-only";
import { serverApi } from "@/shared/lib/api-client.server";
import type { Product } from "../types";

const BASE = "/api/products";
/** Dashboard data must always be fresh: a product edited a second ago is what the bot sells now. */
const FRESH: RequestInit = { cache: "no-store" };

/** Reads for server components: straight to the backend with the signed-in user's token. */
export const productsQueries = {
  list: () => serverApi.get<Product[]>(BASE, FRESH),
  get: (id: string) => serverApi.get<Product>(`${BASE}/${id}`, FRESH),
};
