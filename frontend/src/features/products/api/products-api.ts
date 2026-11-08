import { apiClient } from "@/shared/lib/api-client";
import type { Product, ProductInput } from "../types";

const BASE = "/api/products";

/** Changes from client components (through proxy.ts). Reads live in products-queries.ts. */
export const productsApi = {
  create: (input: ProductInput) => apiClient.post<Product>(BASE, input),
  update: (id: string, input: ProductInput) => apiClient.put<Product>(`${BASE}/${id}`, input),
  remove: (id: string) => apiClient.delete<void>(`${BASE}/${id}`),
};
