import { apiClient } from "@/shared/lib/api-client";
import type { Product, ProductInput } from "../types";

const BASE = "/api/products";
/** Dashboard data must always be fresh: a product edited a second ago is what the bot sells now. */
const FRESH: RequestInit = { cache: "no-store" };

export const productsApi = {
  list: () => apiClient.get<Product[]>(BASE, FRESH),
  get: (id: string) => apiClient.get<Product>(`${BASE}/${id}`, FRESH),
  create: (input: ProductInput) => apiClient.post<Product>(BASE, input),
  update: (id: string, input: ProductInput) => apiClient.put<Product>(`${BASE}/${id}`, input),
  remove: (id: string) => apiClient.delete<void>(`${BASE}/${id}`),
};
